package com.example.registration.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_entry")
public class OutboxEntry
{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, updatable = false)
  private UUID eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private OutboxEventType type;

  @Column(nullable = false, length = 4000, updatable = false)
  private String payload;

  @Column(nullable = false, updatable = false)
  private Instant occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OutboxStatus status;

  protected OutboxEntry()
  {
  }

  private OutboxEntry(OutboxEventType type, String payload)
  {
    this.eventId = UUID.randomUUID();
    this.type = type;
    this.payload = payload;
    this.occurredAt = Instant.now();
    this.status = OutboxStatus.PENDING;
  }

  public static OutboxEntry of(OutboxEventType type, String payload)
  {
    return new OutboxEntry(type, payload);
  }

  public void markFailed()
  {
    this.status = OutboxStatus.FAILED;
  }

  public Long getId()
  {
    return id;
  }

  public UUID getEventId()
  {
    return eventId;
  }

  public OutboxEventType getType()
  {
    return type;
  }

  public String getPayload()
  {
    return payload;
  }

  public Instant getOccurredAt()
  {
    return occurredAt;
  }

  public OutboxStatus getStatus()
  {
    return status;
  }
}
