package com.example.packing.service;

public class ProductNotFoundException extends RuntimeException
{
  public ProductNotFoundException(String id)
  {
    super("The product with id: " + id + " does not exist");
  }
}
