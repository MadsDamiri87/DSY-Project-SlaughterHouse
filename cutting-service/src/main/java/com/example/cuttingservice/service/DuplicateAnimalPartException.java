package com.example.cuttingservice.service;

public class DuplicateAnimalPartException extends RuntimeException
{
  public DuplicateAnimalPartException(String partId)
  {
    super("AnimalPart with id: " + partId + " already exists");
  }
}
