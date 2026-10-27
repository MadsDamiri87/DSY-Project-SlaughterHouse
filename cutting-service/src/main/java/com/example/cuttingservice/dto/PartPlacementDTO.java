package com.example.cuttingservice.dto;

import jakarta.validation.constraints.NotBlank;

public record PartPlacementDTO(

    @NotBlank
    String partId

)
{
}
