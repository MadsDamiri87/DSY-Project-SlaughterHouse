package com.example.cuttingservice.dto;

import com.example.cuttingservice.entity.Tray;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record AnimalPartDTO(

    @NotBlank
    String partId,

    @NotBlank
    String type,

    @NotBlank
    @Positive
    double weight,

    @NotBlank
    int animalRegistrationNumber,

    Instant registrationDateTime,

    @NotBlank
    Tray tray

)
{
}
