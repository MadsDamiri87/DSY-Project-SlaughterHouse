package com.example.cuttingservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Tray
{
  @Id
  private String trayId;

  private String partType;
  private double maxWeight;
  private double currentWeight;

  @OneToMany(mappedBy = "tray")
  private List<AnimalPart> parts = new ArrayList<>();

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
    Tray current = part.getTray();
    if (current != null)
    {
      if (trayId.equals(current.getTrayId()))
      {
        return;
      }
      throw new PartAlreadyPlacedException(part.getPartId(), current.getTrayId());
    }

    assertSamePartType(part);
    assertWithinCapacity(part);

    parts.add(part);
    part.setTray(this);
    currentWeight += part.getWeight();
  }

  private void assertSamePartType(AnimalPart part)
  {
    if (!partType.equals(part.getType()))
    {
      throw new PartTypeMismatchException(trayId, partType, part.getType());
    }
  }

  private void assertWithinCapacity(AnimalPart part)
  {
    double attemptedWeight = currentWeight + part.getWeight();
    if (attemptedWeight > maxWeight)
    {
      throw new TrayCapacityExceededException(trayId, maxWeight, attemptedWeight);
    }
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

  public List<AnimalPart> getParts()
  {
    return List.copyOf(parts);
  }
}
