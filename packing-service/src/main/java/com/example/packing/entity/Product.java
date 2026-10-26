package com.example.packing.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Product
{
  @Id
  private String productId;

  private String productType;
  private Instant packingDateTime;
  private double totalWeight;

  @ElementCollection
  @CollectionTable(name = "product_tray", joinColumns = @JoinColumn(name = "product_id"))
  @Column(name = "tray_id")
  private List<String> trayIds = new ArrayList<>();

  protected Product()
  {
  }

  public Product(String productId, String productType, Instant packingDateTime, double totalWeight,
                 List<String> trayIds)
  {
    this.productId = productId;
    this.productType = productType;
    this.packingDateTime = packingDateTime;
    this.totalWeight = totalWeight;
    this.trayIds = new ArrayList<>(trayIds);
  }

  public String getProductId()
  {
    return productId;
  }

  public String getProductType()
  {
    return productType;
  }

  public Instant getPackingDateTime()
  {
    return packingDateTime;
  }

  public double getTotalWeight()
  {
    return totalWeight;
  }

  public List<String> getTrayIds()
  {
    return trayIds;
  }
}
