package com.example;

import generated.GetAnimalsForProductRequest;
import generated.GetProductsForAnimalRequest;
import generated.TraceabilityServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;

public class Client
{
  public static void main(String[] args) throws InterruptedException
  {
    String target = System.getProperty("target", "localhost:9090");
    String productId = args.length > 0 ? args[0] : "PROD-LOIN-PACK";
    int animalId = args.length > 1 ? Integer.parseInt(args[1]) : 1;

    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

    try
    {
      TraceabilityServiceGrpc.TraceabilityServiceBlockingStub stub =
          TraceabilityServiceGrpc.newBlockingStub(channel);

      try
      {
        var animals = stub.getAnimalsForProduct(
            GetAnimalsForProductRequest.newBuilder().setProductId(productId).build());
        System.out.println("Animals in product " + productId + ": " + animals.getAnimalIdsList());
      }
      catch (StatusRuntimeException e)
      {
        System.out.println("getAnimalsForProduct failed: " + e.getStatus());
      }

      try
      {
        var products = stub.getProductsForAnimal(
            GetProductsForAnimalRequest.newBuilder().setAnimalId(animalId).build());
        System.out.println("Products from animal " + animalId + ": " + products.getProductIdsList());
      }
      catch (StatusRuntimeException e)
      {
        System.out.println("getProductsForAnimal failed: " + e.getStatus());
      }
    }
    finally
    {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
