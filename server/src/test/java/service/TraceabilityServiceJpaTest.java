package service;

import entity.Animal;
import entity.AnimalPart;
import entity.Product;
import entity.Tray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import server.Server;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * Integrationstest: servicen kørt mod en rigtig database.
 *
 * Opgaven kræver at data kommer fra en database, og det er præcis det denne test beviser.
 * Enhedstesten ved siden af mocker repositories væk — her bliver de afledte queries
 * (findByTrayIn, findDistinctByTraysIn) faktisk oversat til SQL og kørt.
 *
 * @DataJpaTest starter kun JPA-delen af Spring (ingen gRPC-server) mod en H2-database
 * i hukommelsen, og ruller hver test tilbage bagefter, så testene er uafhængige.
 * @ContextConfiguration peger på Server, fordi denne testklasse ligger i pakken
 * "service" og Spring ellers ikke selv kan finde konfigurationen.
 * @Import tilføjer TraceabilityService, som @DataJpaTest ellers filtrerer fra.
 */
@DataJpaTest(showSql = false)
@ContextConfiguration(classes = Server.class)
@Import(TraceabilityService.class)
class TraceabilityServiceJpaTest
{
  @Autowired private TestEntityManager em;
  @Autowired private TraceabilityService service;

  /*
   * Bygger et lille slagteri op før hver test:
   *
   *   gris 1 --> lænd  ---   *   gris 2 --> lænd  ----+--> TRAY-LOIN-1 --> PROD-LOIN-PACK og PROD-HALF-ANIMAL
   *   gris 1 --> bryst ----+--> TRAY-BELLY-1 -> PROD-HALF-ANIMAL
   *   gris 3 --> ribben ---+--> TRAY-RIB-1 ---> (intet produkt)
   *
   * Gris 3 er med med vilje: den giver os et dyr der findes, men aldrig er blevet pakket.
   */
  @BeforeEach
  void seedSlaughterhouse()
  {
    Animal pig1 = animal(1);
    Animal pig2 = animal(2);
    Animal pig3 = animal(3);
    em.persist(pig1);
    em.persist(pig2);
    em.persist(pig3);

    Tray loinTray = tray("TRAY-LOIN-1", "LOIN", 50.0);
    Tray bellyTray = tray("TRAY-BELLY-1", "BELLY", 40.0);
    Tray ribTray = tray("TRAY-RIB-1", "RIB", 30.0);

    AnimalPart loinFromPig1 = part("PART-1", "LOIN", 6.2, pig1);
    AnimalPart loinFromPig2 = part("PART-2", "LOIN", 6.8, pig2);
    AnimalPart bellyFromPig1 = part("PART-3", "BELLY", 4.5, pig1);
    AnimalPart ribFromPig3 = part("PART-4", "RIB", 3.1, pig3);

    loinTray.addPart(loinFromPig1);
    loinTray.addPart(loinFromPig2);
    bellyTray.addPart(bellyFromPig1);
    ribTray.addPart(ribFromPig3);

    em.persist(loinTray);
    em.persist(bellyTray);
    em.persist(ribTray);
    em.persist(loinFromPig1);
    em.persist(loinFromPig2);
    em.persist(bellyFromPig1);
    em.persist(ribFromPig3);

    em.persist(product("PROD-LOIN-PACK", "LOIN_PACK", List.of(loinTray)));
    em.persist(product("PROD-HALF-ANIMAL", "HALF_ANIMAL", List.of(loinTray, bellyTray)));

    // flush skriver til databasen, clear tømmer cachen.
    // Uden clear ville servicen risikere at læse objekterne fra hukommelsen,
    // og så testede vi ikke rigtig at queries virker.
    em.flush();
    em.clear();
  }

  // Bakken rummer dele fra gris 1 og 2, så begge skal tilbagekaldes med produktet.
  @Test
  void aProductMadeFromOneTrayTracesBackToEveryAnimalInThatTray()
  {
    assertThat(service.getAnimalsForProduct("PROD-LOIN-PACK")).containsExactlyInAnyOrder(1, 2);
  }

  /*
   * "Halvt dyr" er pakket af to bakker, og gris 1 optræder i dem begge.
   * Svaret skal stadig kun nævne gris 1 én gang.
   */
  @Test
  void aProductMadeFromSeveralTraysTracesBackToEachAnimalOnlyOnce()
  {
    assertThat(service.getAnimalsForProduct("PROD-HALF-ANIMAL")).containsExactlyInAnyOrder(1, 2);
  }

  /*
   * Selve tilbagekaldelsen fra opgaveteksten: er der problemer med gris 1,
   * skal begge produkter kunne kaldes tilbage.
   */
  @Test
  void anAnimalTracesForwardToEveryProductItMightBePartOf()
  {
    assertThat(service.getProductsForAnimal(1))
        .containsExactlyInAnyOrder("PROD-LOIN-PACK", "PROD-HALF-ANIMAL");
  }

  // Gris 3 er skåret op, men dens bakke er aldrig blevet pakket: tom liste, ingen fejl.
  @Test
  void anAnimalWhosePartsWereNeverPackedTracesToNoProducts()
  {
    assertThat(service.getProductsForAnimal(3)).isEmpty();
  }

  // Findes id'et slet ikke, er det en fejl — og ikke bare et tomt resultat.
  @Test
  void anUnknownProductIsRejected()
  {
    assertThatThrownBy(() -> service.getAnimalsForProduct("PROD-DOES-NOT-EXIST"))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void anUnknownAnimalIsRejected()
  {
    assertThatThrownBy(() -> service.getProductsForAnimal(99))
        .isInstanceOf(NoSuchElementException.class);
  }

  // Hjælpemetoder, så opbygningen ovenfor kan læses som et slagteri og ikke som JPA-kode.
  private Animal animal(int id)
  {
    Animal animal = new Animal(id, 100.0);
    animal.setSpecies("Pig");
    animal.setSlaughterDateTime(LocalDateTime.now());
    return animal;
  }

  private Tray tray(String id, String partType, double maxWeight)
  {
    Tray tray = new Tray();
    tray.setTrayId(id);
    tray.setPartType(partType);
    tray.setMaxWeight(maxWeight);
    return tray;
  }

  private AnimalPart part(String id, String type, double weight, Animal animal)
  {
    AnimalPart part = new AnimalPart();
    part.setPartId(id);
    part.setType(type);
    part.setWeight(weight);
    part.setAnimal(animal);
    return part;
  }

  private Product product(String id, String type, List<Tray> trays)
  {
    Product product = new Product();
    product.setProductId(id);
    product.setProductType(type);
    product.setPackingDateTime(LocalDateTime.now());
    product.setTrays(new ArrayList<>(trays));
    return product;
  }
}
