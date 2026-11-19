package com.example.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OutboxEntryDTO(

    Long id,

    String eventId,

    String type,

    String payload,

    Instant occurredAt,

    String status)
{
}
