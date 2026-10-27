package com.example.cuttingservice.service;

public class AnimalPartNotFoundException extends RuntimeException
{
  public AnimalPartNotFoundException(String partId)
  {
    super("AnimalPart with id: " + partId + " was not found");
  }
}
