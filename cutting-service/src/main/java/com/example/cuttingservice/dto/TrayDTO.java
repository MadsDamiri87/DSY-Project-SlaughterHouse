package com.example.cuttingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TrayDTO(

    String trayId,

    @NotBlank String partType,

    @NotBlank @Positive double maxWeight,

    @NotBlank @Positive double currentWeight

)
{
}
