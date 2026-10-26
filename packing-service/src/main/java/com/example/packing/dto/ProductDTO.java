package com.example.packing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public record ProductDTO(

    @NotBlank String productId,

    @NotBlank String productType,

    @NotNull Instant packingDateTime,

    @NotNull @Positive Double totalWeight,

    @NotEmpty List<String> trayIds)
{
}
