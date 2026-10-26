package com.example.packing.service;

public class DuplicateProductException extends RuntimeException
{
  public DuplicateProductException(String id)
  {
    super("The product with id: " + id + " already exists");
  }
}
