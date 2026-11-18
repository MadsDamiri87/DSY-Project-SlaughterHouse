package com.example.registration.dto;

import java.time.Instant;

public record OutboxEntryDTO(

    Long id,

    String eventId,

    String type,

    String payload,

    Instant occurredAt,

    String status)
{
}
