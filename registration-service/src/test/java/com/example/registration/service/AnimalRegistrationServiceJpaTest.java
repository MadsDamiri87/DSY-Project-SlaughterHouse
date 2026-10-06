package com.example.registration.service;

import com.example.registration.entity.Animal;
import com.example.shared.dto.AnimalDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * Integrationstest: servicen kørt mod en rigtig database.
 *
 * Opgaven kræver at oplysningerne kommer fra en database, og det er det denne test beviser.
 * Enhedstesten ved siden af mocker repository væk — her bliver de afledte queries faktisk
 * oversat til SQL og kørt mod en H2-database i hukommelsen.
 *
 * @DataJpaTest starter kun JPA-delen af Spring (ingen webserver) og ruller hver test
 * tilbage bagefter, så testene ikke påvirker hinanden.
 * @Import tilføjer servicen, som @DataJpaTest ellers filtrerer fra.
 */
@DataJpaTest(showSql = false)
@Import(AnimalRegistrationService.class)
class AnimalRegistrationServiceJpaTest
{
  @Autowired private TestEntityManager em;
  @Autowired private AnimalRegistrationService service;

  /*
   * Fire dyr fra tre gårde fordelt på to dage:
   *
   *   1  5. okt kl. 08:30  Nørregaard
   *   2  5. okt kl. 23:59  Nørregaard   <- sent på dagen, skal stadig tælle med
   *   3  6. okt kl. 00:00  Søndergaard  <- præcis midnat, må IKKE tælle med den 5.
   *   4  6. okt kl. 09:15  Vestergaard
   */
  @BeforeEach
  void seedAnimals()
  {
    em.persist(new Animal(1, LocalDateTime.of(2026, 10, 5, 8, 30), 95.0, "Nørregaard"));
    em.persist(new Animal(2, LocalDateTime.of(2026, 10, 5, 23, 59), 102.5, "Nørregaard"));
    em.persist(new Animal(3, LocalDateTime.of(2026, 10, 6, 0, 0), 88.0, "Søndergaard"));
    em.persist(new Animal(4, LocalDateTime.of(2026, 10, 6, 9, 15), 91.0, "Vestergaard"));

    em.flush();
    em.clear();
  }

  // Opslag på ét bestemt dyr.
  @Test
  void aSpecificAnimalCanBeRead()
  {
    AnimalDTO animal = service.findByRegistrationNumber(1);

    assertThat(animal.weight()).isEqualTo(95.0);
    assertThat(animal.origin()).isEqualTo("Nørregaard");
  }

  /*
   * Opslag på ankomstdato. Dyr 2 ankom kl. 23:59 og skal med;
   * dyr 3 ankom kl. 00:00 dagen efter og skal ikke. Det er præcis grænsetilfældet
   * som et inklusivt interval ville have ramt forkert.
   */
  @Test
  void allAnimalsArrivingOnAParticularDateCanBeRead()
  {
    assertThat(service.findByArrivalDate(LocalDate.of(2026, 10, 5)))
        .extracting(AnimalDTO::registrationNumber)
        .containsExactlyInAnyOrder(1, 2);
  }

  // Opslag på gård. To dyr deler gård, så filtreringen kan rent faktisk ses.
  @Test
  void allAnimalsOfAParticularOriginCanBeRead()
  {
    assertThat(service.findByOrigin("Nørregaard"))
        .extracting(AnimalDTO::registrationNumber)
        .containsExactlyInAnyOrder(1, 2);
  }

  // Gårdnavnet skal ikke være versalfølsomt — ellers er opslaget skrøbeligt i praksis.
  @Test
  void originLookupIgnoresCasing()
  {
    assertThat(service.findByOrigin("nørregaard")).hasSize(2);
  }

  // Registrering skrives faktisk til databasen og kan læses igen bagefter.
  @Test
  void aRegisteredAnimalIsStoredAndCanBeReadBack()
  {
    service.register(new AnimalDTO(5, LocalDateTime.of(2026, 10, 7, 7, 0), 99.0, "Østergaard"));
    em.flush();
    em.clear();

    assertThat(service.findByRegistrationNumber(5).origin()).isEqualTo("Østergaard");
  }

  // Samme registreringsnummer to gange skal afvises, ikke overskrive det første dyr.
  @Test
  void registeringAnExistingNumberIsRejected()
  {
    assertThatThrownBy(
        () -> service.register(new AnimalDTO(1, LocalDateTime.now(), 50.0, "Andengaard")))
        .isInstanceOf(DuplicateRegistrationException.class);
  }
}
