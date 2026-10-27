package com.example.cuttingservice.entity;

public class PartTypeMismatchException extends RuntimeException
{
  public PartTypeMismatchException(String trayId, String trayPartType, String partType)
  {
    super("Tray " + trayId + " holds " + trayPartType + " and cannot take a part of type "
          + partType);
  }
}
