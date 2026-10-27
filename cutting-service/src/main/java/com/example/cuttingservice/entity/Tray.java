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

  public Tray(String trayId, String partType, double maxWeight)
  {
    this.trayId        = trayId;
    this.partType      = partType;
    this.maxWeight     = maxWeight;
    this.currentWeight = 0.0;
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

  public String getTrayId()
  {
    return trayId;
  }

  public String getPartType()
  {
    return partType;
  }

  public double getMaxWeight()
  {
    return maxWeight;
  }

  public double getCurrentWeight()
  {
    return currentWeight;
  }
}
