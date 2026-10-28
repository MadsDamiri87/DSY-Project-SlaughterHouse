package com.example.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "registered_animal")
public class Animal
{
  @Id
  @Column(name = "registration_number")
  private int registrationNumber;

  private Instant arrivalDateTime;

  private double weight;

  private String origin;

  public Animal()
  {}

  public Animal(int registrationNumber, Instant arrivalDateTime, double weight, String origin)
  {
    this.registrationNumber = registrationNumber;
    this.arrivalDateTime = arrivalDateTime;
    this.weight = weight;
    this.origin = origin;
  }

  public int getRegistrationNumber()
  {
    return registrationNumber;
  }

  public Instant getArrivalDateTime()
  {
    return arrivalDateTime;
  }

  public double getWeight()
  {
    return weight;
  }

  public String getOrigin()
  {
    return origin;
  }
}
