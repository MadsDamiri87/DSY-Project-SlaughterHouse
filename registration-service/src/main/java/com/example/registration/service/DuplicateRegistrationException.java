package com.example.registration.service;

public class DuplicateRegistrationException extends RuntimeException
{
  public DuplicateRegistrationException(int registrationNumber)
  {
    super("An animal with registration number " + registrationNumber + " is already registered");
  }
}
