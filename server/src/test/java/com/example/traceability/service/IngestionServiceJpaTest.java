package com.example.traceability.service;

import com.example.traceability.dto.AnimalDTO;
import com.example.traceability.dto.AnimalPartDTO;
import com.example.traceability.dto.ProductDTO;
import com.example.traceability.dto.TrayDTO;
import com.example.traceability.entity.Animal;
import com.example.traceability.entity.AnimalPart;
import com.example.traceability.entity.Product;
import com.example.traceability.entity.Tray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Test af den vej data kommer ind i backenden fra stationerne.
 *
 * Det vigtigste her er idempotens. Stationen gensender hvis svaret gik tabt, så
 * den samme besked to gange skal give samme resultat — ikke en dublet og ikke en fejl.
 *
 * Kører mod PostgreSQL ligesom de andre JPA-tests, så upsert-opførslen er den samme
 * som i produktionen.
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(IngestionService.class)
class IngestionServiceJpaTest
{
  @Autowired private TestEntityManager em;
  @Autowired private IngestionService service;

  private static final Instant ARRIVAL = Instant.parse("2026-10-25T08:30:00Z");

  // Et dyr der ikke findes i forvejen skal oprettes med de rapporterede data.
  @Test
  void anUnknownAnimalIsCreated()
  {
    service.receiveAnimal(new AnimalDTO(9001, ARRIVAL, 97.5, "Nørregaard"));
    em.flush();
    em.clear();

    Animal stored = em.find(Animal.class, 9001);
    assertThat(stored.getWeight()).isEqualTo(97.5);
    assertThat(stored.getOrigin()).isEqualTo("Nørregaard");
    assertThat(stored.getArrivalDateTime()).isEqualTo(ARRIVAL);
  }

  /*
   * Kernen i det hele: samme besked to gange må ikke give to rækker.
   * Det virker fordi registreringsnummeret kommer fra stationen, så save() bliver
   * et upsert i stedet for en indsættelse.
   */
  @Test
  void thesameAnimalReportedTwiceIsStoredOnce()
  {
    AnimalDTO reported = new AnimalDTO(9002, ARRIVAL, 88.0, "Søndergaard");

    service.receiveAnimal(reported);
    service.receiveAnimal(reported);
    em.flush();
    em.clear();

    assertThat(em.getEntityManager()
        .createQuery("select count(a) from Animal a where a.registrationNumber = 9002", Long.class)
        .getSingleResult()).isEqualTo(1L);
  }

  // Ankomsttidspunktet skal være det stationen rapporterede, ikke det tidspunkt beskeden nåede frem.
  @Test
  void theReportedArrivalTimeIsKeptEvenWhenTheMessageArrivesLate()
  {
    Instant longAgo = Instant.parse("2026-10-20T06:00:00Z");

    service.receiveAnimal(new AnimalDTO(9003, longAgo, 91.0, "Vestergaard"));
    em.flush();
    em.clear();

    assertThat(em.find(Animal.class, 9003).getArrivalDateTime()).isEqualTo(longAgo);
  }

  // En del skal kobles til både sit dyr og sin bakke, så sporingen kan følge kæden.
  @Test
  void aPartIsLinkedToItsAnimalAndTray()
  {
    service.receiveAnimal(new AnimalDTO(9004, ARRIVAL, 95.0, "Nørregaard"));
    service.receiveTray(new TrayDTO("TRAY-A", "LOIN", 50.0));
    service.receivePart(new AnimalPartDTO("PART-A", "LOIN", 6.2, 9004, "TRAY-A", ARRIVAL));
    em.flush();
    em.clear();

    AnimalPart stored = em.find(AnimalPart.class, "PART-A");
    assertThat(stored.getAnimal().getRegistrationNumber()).isEqualTo(9004);
    assertThat(stored.getTray().getTrayId()).isEqualTo("TRAY-A");
  }

  /*
   * Station 1 og Station 2 har hver sin outbox, så en del kan nå frem før sit dyr.
   * Men findes der en del, ER dyret ankommet — beskeden er bare ikke nået frem endnu.
   * Derfor oprettes dyret som en skal i stedet for at afvise delen.
   */
  @Test
  void aPartForAnUnknownAnimalCreatesAPlaceholder()
  {
    service.receivePart(new AnimalPartDTO("PART-B", "LOIN", 6.2, 99999, null, ARRIVAL));
    em.flush();
    em.clear();

    Animal placeholder = em.find(Animal.class, 99999);
    assertThat(placeholder).isNotNull();
    assertThat(placeholder.getArrivalDateTime()).isNull();
    assertThat(em.find(AnimalPart.class, "PART-B").getAnimal().getRegistrationNumber())
        .isEqualTo(99999);
  }

  /*
   * Og når Station 1's besked så endelig ankommer, skal skallen udfyldes —
   * ikke blive til endnu en række. Det er upsert'en der klarer det.
   */
  @Test
  void aLateAnimalReportFillsInThePlaceholder()
  {
    service.receivePart(new AnimalPartDTO("PART-C", "LOIN", 6.2, 99998, null, ARRIVAL));
    service.receiveAnimal(new AnimalDTO(99998, ARRIVAL, 95.0, "Nørregaard"));
    em.flush();
    em.clear();

    Animal filled = em.find(Animal.class, 99998);
    assertThat(filled.getOrigin()).isEqualTo("Nørregaard");
    assertThat(filled.getArrivalDateTime()).isEqualTo(ARRIVAL);
    assertThat(em.find(AnimalPart.class, "PART-C").getAnimal().getRegistrationNumber())
        .isEqualTo(99998);
  }

  // Samme princip for en bakke et produkt peger på: bakken findes, beskeden mangler bare.
  @Test
  void aProductForAnUnknownTrayCreatesAPlaceholder()
  {
    service.receiveProduct(
        new ProductDTO("PROD-X", "LOIN_PACK", ARRIVAL, 13.0, List.of("TRAY-SENT")));
    em.flush();
    em.clear();

    assertThat(em.find(Tray.class, "TRAY-SENT")).isNotNull();
    assertThat(em.find(Product.class, "PROD-X").getTrays()).hasSize(1);
  }

  // Et produkt skal huske alle de bakker det blev pakket fra — det er dem sporingen går igennem.
  @Test
  void aProductKeepsEveryTrayItWasPackedFrom()
  {
    service.receiveTray(new TrayDTO("TRAY-B", "LOIN", 50.0));
    service.receiveTray(new TrayDTO("TRAY-C", "BELLY", 40.0));
    service.receiveProduct(
        new ProductDTO("PROD-Y", "HALF_ANIMAL", ARRIVAL, 17.5, List.of("TRAY-B", "TRAY-C")));
    em.flush();
    em.clear();

    Product stored = em.find(Product.class, "PROD-Y");
    assertThat(stored.getTrays()).hasSize(2);
  }
}
