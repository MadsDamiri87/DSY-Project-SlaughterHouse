package com.example.desktop.auth;

import java.util.Map;
import java.util.Optional;

public record Session(String username, Role role)
{
  private static final Map<String, Role> USERS = Map.of(
      "station1", Role.STATION_1,
      "station2", Role.STATION_2,
      "station3", Role.STATION_3,
      "admin", Role.ADMIN);

  public static Optional<Session> authenticate(String username, String password)
  {
    if (username == null || password == null)
    {
      return Optional.empty();
    }

    String name = username.trim().toLowerCase();
    Role role = USERS.get(name);

    if (role == null || !name.equals(password))
    {
      return Optional.empty();
    }

    return Optional.of(new Session(name, role));
  }
}
