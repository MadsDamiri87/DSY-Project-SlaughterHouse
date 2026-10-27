package com.example.cuttingservice.service;

import com.example.cuttingservice.dto.AnimalPartDTO;
import com.example.cuttingservice.dto.TrayDTO;
import com.example.cuttingservice.entity.AnimalPart;
import com.example.cuttingservice.entity.PartTypeMismatchException;
import com.example.cuttingservice.entity.Tray;
import com.example.cuttingservice.entity.TrayCapacityExceededException;
import com.example.cuttingservice.outbox.OutboxEntry;
import com.example.cuttingservice.outbox.OutboxEventType;
import com.example.cuttingservice.outbox.OutboxRepository;
import com.example.cuttingservice.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * Integrationstest af opskæringen mod en rigtig database.
 *
 * Det vigtigste er det samme som i pakkestationen: delen eller bakken og
 * outbox-posten skal gemmes i SAMME transaktion. Sker det ikke, kan en del
 * ligge i stationens egen database uden at backenden nogensinde får besked,
 * og så er sporbarheden brudt uden at nogen opdager det.
 *
 * @Import henter JacksonAutoConfiguration med, fordi servicen bruger ObjectMapper
 * til payloaden, og @DataJpaTest ellers ikke stiller en til rådighed.
 */
@DataJpaTest(showSql = false)
@Import({CuttingService.class, JacksonAutoConfiguration.class})
class CuttingServiceJpaTest
{
  @Autowired private TestEntityManager em;
  @Autowired private CuttingService service;
  @Autowired private OutboxRepository outboxRepository;

  private static final Instant CUT_AT = Instant.parse("2026-10-27T08:30:00Z");

  private AnimalPartDTO reportedPart(String partId, String type, double weight)
  {
    return new AnimalPartDTO(partId, type, weight, 9001, CUT_AT, null);
  }

  private List<OutboxEntry> pending()
  {
    return outboxRepository.findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus.PENDING);
  }

  // En registreret del skal kunne læses igen med de data stationen rapporterede.
  @Test
  void aRegisteredPartIsStoredAndCanBeReadBack()
  {
    service.registerPart(reportedPart("PART-1", "LOIN", 6.2));
    em.flush();
    em.clear();

    AnimalPartDTO stored = service.findPartById("PART-1");
    assertThat(stored.type()).isEqualTo("LOIN");
    assertThat(stored.weight()).isEqualTo(6.2);
    assertThat(stored.animalRegistrationNumber()).isEqualTo(9001);
    assertThat(stored.registrationDateTime()).isEqualTo(CUT_AT);
  }

  // Delen og hændelsen skal gemmes sammen, ellers går registreringen tabt.
  @Test
  void registeringAPartAlsoWritesAnOutboxEntry()
  {
    service.registerPart(reportedPart("PART-2", "LOIN", 6.2));
    em.flush();

    assertThat(pending()).hasSize(1);
    assertThat(pending().getFirst().getType()).isEqualTo(OutboxEventType.PART_REGISTERED);
    assertThat(pending().getFirst().getPayload()).contains("PART-2");
  }

  /*
   * Udelades skæretidspunktet, sætter servicen det selv. Men sender stationen
   * et tidspunkt med, skal DET bruges - en del skåret kl. 08:30 er stadig
   * skåret kl. 08:30, selv om beskeden først nåede frem kl. 11.
   */
  @Test
  void theReportedCuttingTimeIsKeptEvenWhenTheMessageArrivesLate()
  {
    Instant longAgo = Instant.parse("2026-10-20T06:00:00Z");

    AnimalPartDTO registered =
        service.registerPart(new AnimalPartDTO("PART-3", "LOIN", 6.2, 9001, longAgo, null));

    assertThat(registered.registrationDateTime()).isEqualTo(longAgo);
  }

  @Test
  void theCuttingTimeIsSetWhenTheCallerLeavesItOut()
  {
    AnimalPartDTO registered =
        service.registerPart(new AnimalPartDTO("PART-4", "LOIN", 6.2, 9001, null, null));

    assertThat(registered.registrationDateTime()).isNotNull();
  }

  // Samme part-id to gange skal afvises, så en gensendt besked ikke bliver en dublet.
  @Test
  void registeringTheSamePartTwiceIsRejected()
  {
    service.registerPart(reportedPart("PART-5", "LOIN", 6.2));
    em.flush();

    assertThatThrownBy(() -> service.registerPart(reportedPart("PART-5", "LOIN", 6.2)))
        .isInstanceOf(DuplicateAnimalPartException.class);
  }

  // En ny bakke oprettes tom, uanset hvad kalderen måtte påstå om vægten.
  @Test
  void aNewTrayIsCreatedEmpty()
  {
    TrayDTO created = service.createTray(new TrayDTO("TRAY-A", "LOIN", 50.0, 42.0));

    assertThat(created.currentWeight()).isZero();
    assertThat(created.maxWeight()).isEqualTo(50.0);
  }

  @Test
  void creatingTheSameTrayTwiceIsRejected()
  {
    service.createTray(new TrayDTO("TRAY-B", "LOIN", 50.0, 0.0));
    em.flush();

    assertThatThrownBy(() -> service.createTray(new TrayDTO("TRAY-B", "LOIN", 50.0, 0.0)))
        .isInstanceOf(DuplicateTrayException.class);
  }

  /*
   * Placeringen er det der binder de to sammen. Bagefter skal delen pege på
   * bakken, bakkens vægt være steget, og backenden skal have tre hændelser i
   * kø: delen blev registreret, bakken blev oprettet, delen blev lagt i bakken.
   */
  @Test
  void placingAPartInATrayLinksThemAndWritesAnEvent()
  {
    service.registerPart(reportedPart("PART-6", "LOIN", 6.2));
    service.createTray(new TrayDTO("TRAY-C", "LOIN", 50.0, 0.0));

    AnimalPartDTO placed = service.placeInTray("PART-6", "TRAY-C");
    em.flush();
    em.clear();

    assertThat(placed.trayId()).isEqualTo("TRAY-C");
    assertThat(em.find(Tray.class, "TRAY-C").getCurrentWeight()).isEqualTo(6.2);
    assertThat(em.find(AnimalPart.class, "PART-6").getTray().getTrayId()).isEqualTo("TRAY-C");
    assertThat(pending()).hasSize(3);
    assertThat(pending()).extracting(OutboxEntry::getType)
        .containsExactlyInAnyOrder(OutboxEventType.PART_REGISTERED,
            OutboxEventType.TRAY_CREATED, OutboxEventType.PART_PLACED_IN_TRAY);
  }

  // Kravet fra opgaven om én slags del pr. bakke skal også holde gennem servicen.
  @Test
  void placingAPartOfTheWrongTypeIsRejected()
  {
    service.registerPart(reportedPart("PART-7", "BELLY", 4.5));
    service.createTray(new TrayDTO("TRAY-D", "LOIN", 50.0, 0.0));

    assertThatThrownBy(() -> service.placeInTray("PART-7", "TRAY-D"))
        .isInstanceOf(PartTypeMismatchException.class);
  }

  @Test
  void placingAPartThatWouldOverfillTheTrayIsRejected()
  {
    service.registerPart(reportedPart("PART-8", "LOIN", 6.2));
    service.createTray(new TrayDTO("TRAY-E", "LOIN", 5.0, 0.0));

    assertThatThrownBy(() -> service.placeInTray("PART-8", "TRAY-E"))
        .isInstanceOf(TrayCapacityExceededException.class);
  }

  // En ukendt del eller bakke er en fejl og ikke et tomt svar, så web-laget kan svare 404.
  @Test
  void anUnknownPartThrows()
  {
    assertThatThrownBy(() -> service.findPartById("PART-FINDES-IKKE"))
        .isInstanceOf(AnimalPartNotFoundException.class)
        .hasMessageContaining("PART-FINDES-IKKE");
  }

  @Test
  void anUnknownTrayThrows()
  {
    assertThatThrownBy(() -> service.findTrayById("TRAY-FINDES-IKKE"))
        .isInstanceOf(TrayNotFoundException.class)
        .hasMessageContaining("TRAY-FINDES-IKKE");
  }
}
