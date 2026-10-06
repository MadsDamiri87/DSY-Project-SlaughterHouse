package com.example.shared.messaging;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AnimalRegisteredEvent(
    UUID eventId,
    int registrationNumber,
    LocalDateTime arrivalDateTime,
    double weight,
    String origin,
    Instant occurredAt)
{
}
