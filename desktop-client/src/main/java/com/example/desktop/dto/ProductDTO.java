package com.example.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDTO(

    String productId,

    String productType,

    Instant packingDateTime,

    Double totalWeight,

    List<String> trayIds)
{
}
