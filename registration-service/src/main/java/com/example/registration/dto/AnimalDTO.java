package com.example.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record AnimalDTO(

    @NotNull @Positive Integer registrationNumber,

    Instant arrivalDateTime,

    @NotNull @Positive Double weight,

    @NotBlank String origin)
{
}
