package com.example;

import generated.GetAnimalTraceRequest;
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
    int registrationNumber = args.length > 1 ? Integer.parseInt(args[1]) : 1;

    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

    try
    {
      TraceabilityServiceGrpc.TraceabilityServiceBlockingStub stub =
          TraceabilityServiceGrpc.newBlockingStub(channel);

      try
      {
        var animals = stub.getAnimalsForProduct(
            GetAnimalsForProductRequest.newBuilder().setProductId(productId).build());
        System.out.println("Animals in product " + productId + ": " + animals.getRegistrationNumbersList());
      }
      catch (StatusRuntimeException e)
      {
        System.out.println("getAnimalsForProduct failed: " + e.getStatus());
      }

      try
      {
        var products = stub.getProductsForAnimal(
            GetProductsForAnimalRequest.newBuilder().setRegistrationNumber(registrationNumber).build());
        System.out.println("Products from animal " + registrationNumber + ": " + products.getProductIdsList());
      }
      catch (StatusRuntimeException e)
      {
        System.out.println("getProductsForAnimal failed: " + e.getStatus());
      }

      try
      {
        var trace = stub.getAnimalTrace(
            GetAnimalTraceRequest.newBuilder().setRegistrationNumber(registrationNumber).build());

        System.out.println("Trace for animal " + trace.getRegistrationNumber()
            + " (" + trace.getSpecies() + ", " + trace.getOrigin() + "):");

        for (var part : trace.getPartsList())
        {
          String tray = part.getTrayId().isEmpty() ? "not in a tray yet" : part.getTrayId();
          String products = part.getProductIdsList().isEmpty()
              ? "not packed yet"
              : "may be in " + String.join(", ", part.getProductIdsList());

          System.out.println("  " + part.getPartId() + "  " + part.getType() + "  "
              + part.getWeight() + " kg  ->  " + tray + "  ->  " + products);
        }
      }
      catch (StatusRuntimeException e)
      {
        System.out.println("getAnimalTrace failed: " + e.getStatus());
      }
    }
    finally
    {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
