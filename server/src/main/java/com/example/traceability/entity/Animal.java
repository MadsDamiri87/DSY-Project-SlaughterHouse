package com.example.traceability.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Animal
{
  @Id
  @Column(name = "animal_id")
  private int registrationNumber;

  private String species;
  private String origin;

  private double weight;
  private LocalDateTime arrivalDateTime = LocalDateTime.now();
  private LocalDateTime slaughterDateTime;

  public Animal()
  {}

  // evt til dummy-data:
  public Animal(int registrationNumber, double weight)
  {
    this.registrationNumber = registrationNumber;
    this.weight = weight;
  }

  public int getRegistrationNumber()
  {
    return registrationNumber;
  }

  public void setRegistrationNumber(int registrationNumber)
  {
    this.registrationNumber = registrationNumber;
  }

  public double getWeight()
  {
    return weight;
  }

  public void setWeight(double weight)
  {
    this.weight = weight;
  }

  public LocalDateTime getArrivalDateTime()
  {
    return arrivalDateTime;
  }

  public void setArrivalDateTime(LocalDateTime arrivalDateTime)
  {
    this.arrivalDateTime = arrivalDateTime;
  }

  public LocalDateTime getSlaughterDateTime()
  {
    return slaughterDateTime;
  }

  public void setSlaughterDateTime(LocalDateTime slaughterDateTime)
  {
    this.slaughterDateTime = slaughterDateTime;
  }

  public String getSpecies()
  {
    return species;
  }

  public void setSpecies(String species)
  {
    this.species = species;
  }

  public String getOrigin()
  {
    return origin;
  }

  public void setOrigin(String origin)
  {
    this.origin = origin;
  }
}
