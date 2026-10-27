package com.example.packing.service;

import com.example.packing.dto.ProductDTO;
import com.example.packing.entity.Product;
import com.example.packing.outbox.OutboxEntry;
import com.example.packing.outbox.OutboxEventType;
import com.example.packing.outbox.OutboxStatus;
import com.example.packing.outbox.OutboxRepository;
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
 * Integrationstest af pakningen mod en rigtig database.
 *
 * Det vigtigste er at produktet og outbox-posten bliver gemt i SAMME transaktion.
 * Sker det ikke, kan et produkt ende i databasen uden at backenden nogensinde
 * får besked - og så er sporbarheden brudt uden at nogen opdager det.
 *
 * @Import henter JacksonAutoConfiguration med, fordi servicen bruger ObjectMapper
 * til at serialisere payloaden, og @DataJpaTest ellers ikke stiller en til rådighed.
 */
@DataJpaTest(showSql = false)
@Import({PackingService.class, JacksonAutoConfiguration.class})
class PackingServiceJpaTest
{
  @Autowired private TestEntityManager em;
  @Autowired private PackingService service;
  @Autowired private OutboxRepository outboxRepository;

  private static final Instant PACKED_AT = Instant.parse("2026-10-26T09:00:00Z");

  // Et pakket produkt skal kunne læses igen med sine bakker.
  @Test
  void aPackedProductIsStoredAndCanBeReadBack()
  {
    service.pack(new ProductDTO("PROD-1", "LOIN_PACK", PACKED_AT, 13.0,
        List.of("TRAY-A", "TRAY-B")));
    em.flush();
    em.clear();

    ProductDTO stored = service.findById("PROD-1");
    assertThat(stored.productType()).isEqualTo("LOIN_PACK");
    assertThat(stored.trayIds()).containsExactly("TRAY-A", "TRAY-B");
  }

  // Produktet og hændelsen skal gemmes sammen, ellers går registreringen tabt.
  @Test
  void packingAlsoWritesAnOutboxEntry()
  {
    service.pack(new ProductDTO("PROD-2", "HALF_ANIMAL", PACKED_AT, 17.5, List.of("TRAY-C")));
    em.flush();

    List<OutboxEntry> pending =
        outboxRepository.findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus.PENDING);

    assertThat(pending).hasSize(1);
    assertThat(pending.getFirst().getType()).isEqualTo(OutboxEventType.PRODUCT_PACKED);
    assertThat(pending.getFirst().getPayload()).contains("PROD-2");
  }

  // Udelades pakketidspunktet, sætter servicen det selv - et produkt uden dato kan ikke spores.
  @Test
  void packingSetsTheTimeWhenTheCallerLeavesItOut()
  {
    ProductDTO packed =
        service.pack(new ProductDTO("PROD-3", "LOIN_PACK", null, 9.0, List.of("TRAY-D")));

    assertThat(packed.packingDateTime()).isNotNull();
  }

  /*
   * Samme produkt-id to gange skal afvises. Og der må hverken komme et produkt
   * eller en hændelse ud af det - ellers ville backenden få besked om noget
   * der aldrig blev pakket.
   */
  @Test
  void packingTheSameProductTwiceIsRejected()
  {
    service.pack(new ProductDTO("PROD-4", "LOIN_PACK", PACKED_AT, 13.0, List.of("TRAY-E")));
    em.flush();

    assertThatThrownBy(() -> service.pack(
        new ProductDTO("PROD-4", "LOIN_PACK", PACKED_AT, 13.0, List.of("TRAY-E"))))
        .isInstanceOf(DuplicateProductException.class);
  }

  // Et ukendt produkt er en fejl og ikke et tomt svar, så web-laget kan svare 404.
  @Test
  void anUnknownProductThrows()
  {
    assertThatThrownBy(() -> service.findById("PROD-FINDES-IKKE"))
        .isInstanceOf(ProductNotFoundException.class)
        .hasMessageContaining("PROD-FINDES-IKKE");
  }

  // Bakke-id'erne gemmes som værdier og ikke som referencer til Station 2's bakker.
  @Test
  void trayIdsAreStoredAsPlainValues()
  {
    service.pack(new ProductDTO("PROD-5", "HALF_ANIMAL", PACKED_AT, 20.0,
        List.of("TRAY-F", "TRAY-G", "TRAY-H")));
    em.flush();
    em.clear();

    assertThat(em.find(Product.class, "PROD-5").getTrayIds()).hasSize(3);
  }
}
