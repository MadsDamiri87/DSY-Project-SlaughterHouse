package com.example.desktop.domain;

import java.util.List;

public enum ProductType
{
  HALF_ANIMAL("Half animal"),
  LOIN_PACK("Loin pack"),
  BELLY_PACK("Belly pack"),
  RIB_PACK("Rib pack"),
  SHOULDER_PACK("Shoulder pack"),
  MIXED_PACK("Mixed pack");

  private final String displayName;

  ProductType(String displayName)
  {
    this.displayName = displayName;
  }

  public String displayName()
  {
    return displayName;
  }

  public static List<ProductType> all()
  {
    return List.of(values());
  }

  @Override
  public String toString()
  {
    return displayName;
  }
}
