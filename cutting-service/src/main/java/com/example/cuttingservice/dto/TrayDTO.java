package com.example.cuttingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TrayDTO(

    @NotBlank
    String trayId,

    @NotBlank
    String partType,

    @Positive
    double maxWeight,

    double currentWeight

)
{
}
