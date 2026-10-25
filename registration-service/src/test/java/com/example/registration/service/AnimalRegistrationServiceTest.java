package com.example.registration.service;

import com.example.registration.entity.Animal;
import com.example.registration.outbox.OutboxEntry;
import com.example.registration.outbox.OutboxEventType;
import com.example.registration.outbox.OutboxStatus;
import com.example.registration.outbox.OutboxRepository;
import com.example.registration.repository.AnimalRepository;
import com.example.registration.dto.AnimalDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Enhedstest af registreringslogikken.
 *
 * Repository er en Mockito-mock, så der hverken er database eller HTTP med.
 * Fejler en test her, ved vi at fejlen ligger i servicen — ikke i SQL eller i web-laget.
 */
@ExtendWith(MockitoExtension.class)
class AnimalRegistrationServiceTest
{
  @Mock private AnimalRepository animalRepository;
  @Mock private OutboxRepository outboxRepository;

  /*
   * En rigtig ObjectMapper og ikke en mock: vi vil se at payloaden faktisk kan
   * serialiseres. JavaTimeModule er noedvendigt, fordi AnimalDTO har en LocalDateTime.
   */
  @Spy private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @InjectMocks private AnimalRegistrationService service;

  // Registrering af et nyt dyr skal gemme det og give de samme data tilbage.
  @Test
  void registerStoresANewAnimal()
  {
    when(animalRepository.existsById(1)).thenReturn(false);
    when(animalRepository.save(any(Animal.class))).thenAnswer(call -> call.getArgument(0));

    LocalDateTime arrival = LocalDateTime.of(2026, 10, 5, 8, 30);
    AnimalDTO result = service.register(new AnimalDTO(1, arrival, 95.0, "Nørregaard"));

    assertThat(result.registrationNumber()).isEqualTo(1);
    assertThat(result.arrivalDateTime()).isEqualTo(arrival);
    assertThat(result.weight()).isEqualTo(95.0);
    assertThat(result.origin()).isEqualTo("Nørregaard");

    ArgumentCaptor<Animal> savedAnimal = ArgumentCaptor.forClass(Animal.class);
    verify(animalRepository).save(savedAnimal.capture());
    assertThat(savedAnimal.getValue().getRegistrationNumber()).isEqualTo(1);
  }

  /*
   * Dyret og haendelsen skal gemmes i samme kald. Uden outbox-posten ville
   * registreringen aldrig naa backenden, og ingen ville opdage det.
   */
  @Test
  void registerAlsoWritesAnOutboxEntry()
  {
    when(animalRepository.existsById(3)).thenReturn(false);
    when(animalRepository.save(any(Animal.class))).thenAnswer(call -> call.getArgument(0));

    service.register(new AnimalDTO(3, LocalDateTime.of(2026, 10, 5, 8, 30), 95.0, "Noerregaard"));

    ArgumentCaptor<OutboxEntry> entry = ArgumentCaptor.forClass(OutboxEntry.class);
    verify(outboxRepository).save(entry.capture());

    assertThat(entry.getValue().getType()).isEqualTo(OutboxEventType.ANIMAL_REGISTERED);
    assertThat(entry.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(entry.getValue().getPayload()).contains("\"registrationNumber\":3");
  }

  /*
   * Afvises registreringsnummeret, maa der hverken komme et dyr eller en haendelse.
   * Ellers ville backenden faa besked om et dyr der aldrig blev oprettet.
   */
  @Test
  void aRejectedRegistrationWritesNothingAtAll()
  {
    when(animalRepository.existsById(1)).thenReturn(true);

    assertThatThrownBy(() -> service.register(new AnimalDTO(1, null, 95.0, "Noerregaard")))
        .isInstanceOf(DuplicateRegistrationException.class);

    verify(animalRepository, never()).save(any());
    verify(outboxRepository, never()).save(any());
  }

  /*
   * Ankomsttidspunkt er valgfrit i kaldet. Udelades det, skal servicen selv sætte det,
   * så et dyr aldrig ender i databasen uden dato.
   */
  @Test
  void registerSetsArrivalTimeWhenTheCallerLeavesItOut()
  {
    when(animalRepository.existsById(2)).thenReturn(false);
    when(animalRepository.save(any(Animal.class))).thenAnswer(call -> call.getArgument(0));

    AnimalDTO result = service.register(new AnimalDTO(2, null, 88.0, "Søndergaard"));

    assertThat(result.arrivalDateTime()).isNotNull();
  }

  /*
   * Et registreringsnummer må kun bruges én gang.
   * Uden dette tjek ville save() stilfærdigt overskrive det eksisterende dyr.
   */
  @Test
  void registerRejectsARegistrationNumberThatIsAlreadyUsed()
  {
    when(animalRepository.existsById(1)).thenReturn(true);

    assertThatThrownBy(() -> service.register(new AnimalDTO(1, null, 95.0, "Nørregaard")))
        .isInstanceOf(DuplicateRegistrationException.class);

    verify(animalRepository, never()).save(any());
  }

  // Et ukendt dyr er en fejl, ikke et tomt svar — så web-laget kan svare 404.
  @Test
  void findByRegistrationNumberThrowsWhenTheAnimalIsUnknown()
  {
    when(animalRepository.findById(99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findByRegistrationNumber(99))
        .isInstanceOf(AnimalNotFoundException.class)
        .hasMessageContaining("99");
  }

  /*
   * Her er den vigtige detalje: arrivalDateTime er et tidsstempel, men opslaget sker på en dato.
   * Servicen skal derfor oversætte datoen til et halvåbent interval [00:00 den dag, 00:00 dagen efter).
   */
  @Test
  void findByArrivalDateTranslatesTheDateIntoAHalfOpenInterval()
  {
    when(animalRepository.findByArrivalDateTimeGreaterThanEqualAndArrivalDateTimeLessThan(any(), any()))
        .thenReturn(List.of());

    service.findByArrivalDate(LocalDate.of(2026, 10, 5));

    ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(animalRepository)
        .findByArrivalDateTimeGreaterThanEqualAndArrivalDateTimeLessThan(from.capture(), to.capture());

    assertThat(from.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 5, 0, 0));
    assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 6, 0, 0));
  }
}
