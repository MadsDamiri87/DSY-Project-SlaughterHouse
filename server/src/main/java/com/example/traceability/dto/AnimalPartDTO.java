package com.example.traceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record AnimalPartDTO(

    @NotBlank String partId,

    @NotBlank String type,

    @NotNull @Positive Double weight,

    @NotNull @Positive Integer animalRegistrationNumber,

    String trayId,

    @NotNull Instant registrationDateTime)
{
}
