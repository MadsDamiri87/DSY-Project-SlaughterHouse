package com.example.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrayDTO(

    String trayId,

    String partType,

    double maxWeight,

    double currentWeight)
{
  public double remaining()
  {
    return maxWeight - currentWeight;
  }

  public double fillRatio()
  {
    return maxWeight <= 0 ? 0 : Math.min(1.0, currentWeight / maxWeight);
  }
}
