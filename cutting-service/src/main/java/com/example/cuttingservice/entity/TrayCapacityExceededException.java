package com.example.cuttingservice.entity;

public class TrayCapacityExceededException extends RuntimeException
{
  public TrayCapacityExceededException(String trayId, double maxWeight, double attemptedWeight)
  {
    super("Tray " + trayId + " holds at most " + maxWeight + " kg and the part would bring it to "
          + attemptedWeight + " kg");
  }
}
