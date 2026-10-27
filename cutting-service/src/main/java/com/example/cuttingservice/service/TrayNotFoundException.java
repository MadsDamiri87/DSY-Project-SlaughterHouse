package com.example.cuttingservice.service;

public class TrayNotFoundException extends RuntimeException
{
  public TrayNotFoundException(String trayId)
  {
    super("Tray with id: " + trayId + " was not found");
  }
}
