package com.example.desktop.domain;

import com.example.desktop.auth.Role;

import java.util.List;

public enum Station
{
  REGISTRATION(1, "Station 1", "ST 1", Role.STATION_1),
  CUTTING(2, "Station 2", "ST 2", Role.STATION_2),
  PACKING(3, "Station 3", "ST 3", Role.STATION_3);

  private final int number;
  private final String displayName;
  private final String shortName;
  private final Role role;

  Station(int number, String displayName, String shortName, Role role)
  {
    this.number = number;
    this.displayName = displayName;
    this.shortName = shortName;
    this.role = role;
  }

  public int number()
  {
    return number;
  }

  public String displayName()
  {
    return displayName;
  }

  public String shortName()
  {
    return shortName;
  }

  public Role role()
  {
    return role;
  }

  public static List<Station> all()
  {
    return List.of(values());
  }
}
