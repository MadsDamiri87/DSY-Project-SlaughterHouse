package com.example.cuttingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record AnimalPartDTO(

    @NotBlank
    String partId,

    @NotBlank
    String type,

    @Positive
    double weight,

    @NotNull
    @Positive
    Integer animalRegistrationNumber,

    Instant registrationDateTime,

    String trayId

)
{
}
