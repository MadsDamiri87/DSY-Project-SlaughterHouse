package entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Animal
{
  @Id
  private int animalId;

  private String species;

  private double weight;
  private LocalDateTime arrivalDateTime = LocalDateTime.now();
  private LocalDateTime slaughterDateTime;

  public Animal()
  {}

  // evt til dummy-data:
  public Animal(int animalId, double weight)
  {
    this.animalId = animalId;
    this.weight = weight;
  }

  public int getAnimalId()
  {
    return animalId;
  }

  public void setAnimalId(int animalId)
  {
    this.animalId = animalId;
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
}
