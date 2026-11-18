package com.example.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalPartDTO(

    String partId,

    String type,

    double weight,

    Integer animalRegistrationNumber,

    Instant registrationDateTime,

    String trayId)
{
}
