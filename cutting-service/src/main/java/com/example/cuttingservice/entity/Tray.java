package com.example.cuttingservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Tray
{
  @Id
  private String trayId;

  private String partType;
  private double maxWeight;
  private double currentWeight;

  public Tray()
  {
  }

  public Tray(String trayId, String partType, double maxWeight, double currentWeight)
  {
    this.trayId        = trayId;
    this.partType      = partType;
    this.maxWeight     = maxWeight;
    this.currentWeight = currentWeight;
  }

  public void addPart(AnimalPart part)
  {
  }

  private void assertSamePartType(AnimalPart part)
  {

  }

  private void assertWithinCapacity(AnimalPart part)
  {

  }

}
