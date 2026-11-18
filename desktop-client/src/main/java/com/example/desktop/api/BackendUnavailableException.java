package com.example.desktop.api;

public class BackendUnavailableException extends RuntimeException
{
  public BackendUnavailableException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
