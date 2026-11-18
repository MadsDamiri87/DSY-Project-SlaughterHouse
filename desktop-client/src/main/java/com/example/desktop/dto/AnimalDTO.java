package com.example.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalDTO(

    Integer registrationNumber,

    Instant arrivalDateTime,

    Double weight,

    String origin)
{
}
