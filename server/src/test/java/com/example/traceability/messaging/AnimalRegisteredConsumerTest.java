package com.example.traceability.messaging;

import com.example.shared.messaging.AnimalRegisteredEvent;
import com.example.traceability.entity.Animal;
import com.example.traceability.repository.AnimalRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimalRegisteredConsumerTest
{
  private final AnimalRepository animalRepository = mock(AnimalRepository.class);
  private final AnimalRegisteredConsumer consumer = new AnimalRegisteredConsumer(animalRepository);

  @Test
  void createsTraceabilityAnimalUsingRegistrationNumberAsSharedIdentity()
  {
    LocalDateTime arrival = LocalDateTime.of(2026, 10, 6, 8, 30);
    AnimalRegisteredEvent event = event(42, arrival, 95.5, "Nørregaard");
    when(animalRepository.findById(42)).thenReturn(Optional.empty());
    when(animalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    consumer.consume(event);

    var captor = org.mockito.ArgumentCaptor.forClass(Animal.class);
    verify(animalRepository).save(captor.capture());
    Animal saved = captor.getValue();
    assertThat(saved.getAnimalId()).isEqualTo(42);
    assertThat(saved.getArrivalDateTime()).isEqualTo(arrival);
    assertThat(saved.getWeight()).isEqualTo(95.5);
    assertThat(saved.getOrigin()).isEqualTo("Nørregaard");
  }

  @Test
  void updatesExistingAnimalSoRepeatedDeliveryIsIdempotent()
  {
    Animal existing = new Animal(42, 80.0);
    when(animalRepository.findById(42)).thenReturn(Optional.of(existing));

    consumer.consume(event(42, LocalDateTime.of(2026, 10, 6, 8, 30), 95.5, "Nørregaard"));

    verify(animalRepository).save(existing);
    assertThat(existing.getWeight()).isEqualTo(95.5);
    assertThat(existing.getOrigin()).isEqualTo("Nørregaard");
  }

  private AnimalRegisteredEvent event(int registrationNumber, LocalDateTime arrival,
                                      double weight, String origin)
  {
    return new AnimalRegisteredEvent(UUID.randomUUID(), registrationNumber, arrival, weight,
        origin, Instant.now());
  }
}
