package com.example.cuttingservice.entity;

public class PartAlreadyPlacedException extends RuntimeException
{
  public PartAlreadyPlacedException(String partId, String trayId)
  {
    super("AnimalPart " + partId + " is already placed in tray " + trayId);
  }
}
