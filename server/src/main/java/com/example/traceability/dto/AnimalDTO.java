package com.example.traceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record AnimalDTO(

    @NotNull @Positive Integer registrationNumber,

    @NotNull Instant arrivalDateTime,

    @NotNull @Positive Double weight,

    @NotBlank String origin)
{
}
