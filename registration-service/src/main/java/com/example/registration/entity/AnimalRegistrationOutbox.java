package com.example.registration.entity;

import com.example.shared.messaging.AnimalRegisteredEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "animal_registration_outbox")
public class AnimalRegistrationOutbox
{
  @Id
  private UUID eventId;

  private int registrationNumber;
  private LocalDateTime arrivalDateTime;
  private double weight;
  private String origin;
  private Instant occurredAt;

  public AnimalRegistrationOutbox()
  {
  }

  public AnimalRegistrationOutbox(UUID eventId, int registrationNumber,
                                  LocalDateTime arrivalDateTime, double weight,
                                  String origin, Instant occurredAt)
  {
    this.eventId = eventId;
    this.registrationNumber = registrationNumber;
    this.arrivalDateTime = arrivalDateTime;
    this.weight = weight;
    this.origin = origin;
    this.occurredAt = occurredAt;
  }

  public static AnimalRegistrationOutbox from(Animal animal)
  {
    return new AnimalRegistrationOutbox(UUID.randomUUID(), animal.getRegistrationNumber(),
        animal.getArrivalDateTime(), animal.getWeight(), animal.getOrigin(), Instant.now());
  }

  public AnimalRegisteredEvent toEvent()
  {
    return new AnimalRegisteredEvent(eventId, registrationNumber, arrivalDateTime, weight, origin,
        occurredAt);
  }

  public UUID getEventId()
  {
    return eventId;
  }

  public Instant getOccurredAt()
  {
    return occurredAt;
  }
}
