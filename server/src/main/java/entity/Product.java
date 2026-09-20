package entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Product
{

  @Id private String productId;

  private String productType;
  private LocalDateTime packingDateTime;

  private double totalWeight;
  private String sourcePart;

  @ManyToMany private List<Tray> trays = new ArrayList<>();

  public String getProductId()
  {
    return productId;
  }

  public void setProductId(String productId)
  {
    this.productId = productId;
  }

  public String getProductType()
  {
    return productType;
  }

  public void setProductType(String productType)
  {
    this.productType = productType;
  }

  public LocalDateTime getPackingDateTime()
  {
    return packingDateTime;
  }

  public void setPackingDateTime(LocalDateTime packingDateTime)
  {
    this.packingDateTime = packingDateTime;
  }

  public List<Tray> getTrays()
  {
    return trays;
  }

  public void setTrays(List<Tray> trays)
  {
    this.trays = trays;
  }

  public String getSourcePart()
  {
    return sourcePart;
  }

  public void setSourcePart(String sourcePart)
  {
    this.sourcePart = sourcePart;
  }

  public double getTotalWeight()
  {
    return totalWeight;
  }

  public void setTotalWeight(double totalWeight)
  {
    this.totalWeight = totalWeight;
  }
}
