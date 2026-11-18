package com.example.desktop.domain;

import java.util.Collection;

public final class Ids
{
  public static final String ANIMAL_PREFIX = "ANIMAL-";
  public static final String PART_PREFIX = "PART-";
  public static final String TRAY_PREFIX = "TRAY-";
  public static final String PRODUCT_PREFIX = "PROD-";

  private Ids()
  {
  }

  public static String format(String prefix, int number)
  {
    return prefix + String.format("%05d", number);
  }

  public static String animalLabel(Integer registrationNumber)
  {
    return registrationNumber == null ? "" : format(ANIMAL_PREFIX, registrationNumber);
  }

  public static Integer numberIn(String id, String prefix)
  {
    if (id == null || !id.startsWith(prefix))
    {
      return null;
    }

    try
    {
      return Integer.valueOf(id.substring(prefix.length()));
    }
    catch (NumberFormatException exception)
    {
      return null;
    }
  }

  public static int nextNumber(Collection<String> existingIds, String prefix)
  {
    int highest = 0;

    for (String id : existingIds)
    {
      Integer number = numberIn(id, prefix);
      if (number != null && number > highest)
      {
        highest = number;
      }
    }

    return highest + 1;
  }

  public static String next(Collection<String> existingIds, String prefix)
  {
    return format(prefix, nextNumber(existingIds, prefix));
  }

  public static int nextRegistrationNumber(Collection<Integer> existingNumbers)
  {
    int highest = 0;

    for (Integer number : existingNumbers)
    {
      if (number != null && number > highest)
      {
        highest = number;
      }
    }

    return highest + 1;
  }
}
