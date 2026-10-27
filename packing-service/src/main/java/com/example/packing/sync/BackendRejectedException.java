package com.example.packing.sync;

public class BackendRejectedException extends RuntimeException
{
  public BackendRejectedException(String message)
  {
    super(message);
  }

  public BackendRejectedException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
