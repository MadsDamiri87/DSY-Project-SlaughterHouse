package com.example.traceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TrayDTO(

    @NotBlank String trayId,

    @NotBlank String partType,

    @NotNull @Positive Double maxWeight,

    Double currentWeight)
{
}
