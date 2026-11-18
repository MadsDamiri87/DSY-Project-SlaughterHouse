package com.example.desktop.auth;

public enum Role
{
  STATION_1("Station 01 — Registrering"),
  STATION_2("Station 02 — Opskæring"),
  STATION_3("Station 03 — Pakning"),
  ADMIN("Administrator");

  private final String displayName;

  Role(String displayName)
  {
    this.displayName = displayName;
  }

  public String displayName()
  {
    return displayName;
  }

  public boolean isAdmin()
  {
    return this == ADMIN;
  }

  public boolean mayUse(Role station)
  {
    return isAdmin() || this == station;
  }
}
