package com.example.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record AnimalDTO(

    @NotNull Integer registrationNumber,

    LocalDateTime arrivalDateTime,

    @NotNull @Positive Double weight,

    @NotBlank String origin)
{
}
