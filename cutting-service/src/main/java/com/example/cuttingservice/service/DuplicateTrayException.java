package com.example.cuttingservice.service;

public class DuplicateTrayException extends RuntimeException
{
  public DuplicateTrayException(String trayId)
  {
    super("Tray with id: " + trayId + " already exists");
  }
}
