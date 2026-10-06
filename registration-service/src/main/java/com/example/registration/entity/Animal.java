package com.example.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "registered_animal")
public class Animal
{
  @Id
  @Column(name = "registration_number")
  private int registrationNumber;

  private LocalDateTime arrivalDateTime;

  private double weight;

  private String origin;

  public Animal()
  {}

  public Animal(int registrationNumber, LocalDateTime arrivalDateTime, double weight, String origin)
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

  public void setRegistrationNumber(int registrationNumber)
  {
    this.registrationNumber = registrationNumber;
  }

  public LocalDateTime getArrivalDateTime()
  {
    return arrivalDateTime;
  }

  public void setArrivalDateTime(LocalDateTime arrivalDateTime)
  {
    this.arrivalDateTime = arrivalDateTime;
  }

  public double getWeight()
  {
    return weight;
  }

  public void setWeight(double weight)
  {
    this.weight = weight;
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
