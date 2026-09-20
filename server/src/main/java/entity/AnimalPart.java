package entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class AnimalPart
{
  @Id
  private String partId;

  private String type;
  private double weight;

  private LocalDateTime registrationDateTime;

  @ManyToOne private Animal animal;

  @ManyToOne private Tray tray;

  public String getPartId()
  {
    return partId;
  }

  public void setPartId(String partId)
  {
    this.partId = partId;
  }

  public String getType()
  {
    return type;
  }

  public void setType(String type)
  {
    this.type = type;
  }

  public double getWeight()
  {
    return weight;
  }

  public void setWeight(double weight)
  {
    this.weight = weight;
  }

  public Animal getAnimal()
  {
    return animal;
  }

  public void setAnimal(Animal animal)
  {
    this.animal = animal;
  }

  public Tray getTray()
  {
    return tray;
  }

  public void setTray(Tray tray)
  {
    this.tray = tray;
  }
}
