package com.example.cuttingservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

@Entity
public class AnimalPart
{
  @Id
  private String partId;

  private String type;
  private double weight;
  private int animalRegistrationNumber;
  private Instant registrationDateTime;

  @ManyToOne
  @JoinColumn(name = "tray_id")
  private Tray tray;

  protected AnimalPart()
  {
  }

  public AnimalPart(String partId, String type, double weight, int animalRegistrationNumber, Instant registrationDateTime)
  {
    this.partId                  = partId;
    this.type                    = type;
    this.weight                  = weight;
    this.animalRegistrationNumber = animalRegistrationNumber;
    this.registrationDateTime = registrationDateTime;
  }

  public String getPartId()
  {
    return partId;
  }

  public String getType()
  {
    return type;
  }

  public double getWeight()
  {
    return weight;
  }

  public int getAnimalRegistrationNumber()
  {
    return animalRegistrationNumber;
  }

  public Instant getRegistrationDateTime()
  {
    return registrationDateTime;
  }

  public Tray getTray()
  {
    return tray;
  }

  void setTray(Tray tray)
  {
    this.tray = tray;
  }
}
