package com.example.desktop.domain;

import java.util.List;

public enum PartType
{
  LOIN("Loin"),
  BELLY("Belly"),
  RIB("Rib"),
  SHOULDER("Shoulder");

  private final String displayName;

  PartType(String displayName)
  {
    this.displayName = displayName;
  }

  public String displayName()
  {
    return displayName;
  }

  public static List<PartType> all()
  {
    return List.of(values());
  }

  @Override
  public String toString()
  {
    return displayName;
  }
}
