package com.example.registration.service;

public class AnimalNotFoundException extends RuntimeException
{
  public AnimalNotFoundException(int registrationNumber)
  {
    super("No animal with registration number " + registrationNumber);
  }
}
