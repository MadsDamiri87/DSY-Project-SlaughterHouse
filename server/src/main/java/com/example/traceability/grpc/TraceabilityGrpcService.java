package com.example.traceability.grpc;

import generated.GetAnimalTraceRequest;
import generated.GetAnimalTraceResponse;
import generated.GetAnimalsForProductRequest;
import generated.GetAnimalsForProductResponse;
import generated.GetPartTraceRequest;
import generated.GetPartTraceResponse;
import generated.GetProductsForAnimalRequest;
import generated.GetProductsForAnimalResponse;
import generated.TracedPart;
import generated.TraceabilityServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import com.example.traceability.service.AnimalTrace;
import com.example.traceability.service.PartTrace;
import com.example.traceability.service.TraceabilityService;

import java.time.Instant;
import java.util.NoSuchElementException;

@Service
public class TraceabilityGrpcService extends TraceabilityServiceGrpc.TraceabilityServiceImplBase
{
  private final TraceabilityService traceabilityService;

  public TraceabilityGrpcService(TraceabilityService traceabilityService)
  {
    this.traceabilityService = traceabilityService;
  }

  @Override public void getAnimalsForProduct(GetAnimalsForProductRequest request,
                                             StreamObserver<GetAnimalsForProductResponse> responseObserver)
  {
    if (request.getProductId().isBlank())
    {
      responseObserver.onError(Status.INVALID_ARGUMENT
          .withDescription("productId must not be blank")
          .asRuntimeException());
      return;
    }

    try
    {
      var registrationNumbers = traceabilityService.getAnimalsForProduct(request.getProductId());

      var response =
          GetAnimalsForProductResponse.newBuilder().addAllRegistrationNumbers(registrationNumbers).build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }
    catch (NoSuchElementException e)
    {
      responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    }
    catch (RuntimeException e)
    {
      responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override public void getAnimalTrace(GetAnimalTraceRequest request,
                                       StreamObserver<GetAnimalTraceResponse> responseObserver)
  {
    if (request.getRegistrationNumber() <= 0)
    {
      responseObserver.onError(Status.INVALID_ARGUMENT
          .withDescription("registrationNumber must be a positive number")
          .asRuntimeException());
      return;
    }

    try
    {
      AnimalTrace trace = traceabilityService.traceAnimal(request.getRegistrationNumber());

      var builder = GetAnimalTraceResponse.newBuilder()
          .setRegistrationNumber(trace.registrationNumber())
          .setSpecies(text(trace.species()))
          .setOrigin(text(trace.origin()))
          .setWeight(trace.weight())
          .setArrivalDateTime(text(trace.arrivalDateTime()));

      trace.parts().forEach(part -> builder.addParts(toProto(part)));

      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
    }
    catch (NoSuchElementException e)
    {
      responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    }
    catch (RuntimeException e)
    {
      responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override public void getPartTrace(GetPartTraceRequest request,
                                     StreamObserver<GetPartTraceResponse> responseObserver)
  {
    if (request.getPartId().isBlank())
    {
      responseObserver.onError(Status.INVALID_ARGUMENT
          .withDescription("partId must not be blank")
          .asRuntimeException());
      return;
    }

    try
    {
      PartTrace trace = traceabilityService.tracePart(request.getPartId());

      var response = GetPartTraceResponse.newBuilder()
          .setPart(toProto(trace.part()))
          .setRegistrationNumber(trace.registrationNumber())
          .setSpecies(text(trace.species()))
          .setOrigin(text(trace.origin()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }
    catch (NoSuchElementException e)
    {
      responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    }
    catch (RuntimeException e)
    {
      responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  private TracedPart toProto(com.example.traceability.service.TracedPart part)
  {
    return TracedPart.newBuilder()
        .setPartId(text(part.partId()))
        .setType(text(part.type()))
        .setWeight(part.weight())
        .setRegistrationDateTime(text(part.registrationDateTime()))
        .setTrayId(text(part.trayId()))
        .addAllProductIds(part.productIds())
        .build();
  }

  private String text(String value)
  {
    return value == null ? "" : value;
  }

  private String text(Instant value)
  {
    return value == null ? "" : value.toString();
  }

  @Override public void getProductsForAnimal(GetProductsForAnimalRequest request,
                                             StreamObserver<GetProductsForAnimalResponse> responseObserver)
  {
    if (request.getRegistrationNumber() <= 0)
    {
      responseObserver.onError(Status.INVALID_ARGUMENT
          .withDescription("registrationNumber must be a positive number")
          .asRuntimeException());
      return;
    }

    try
    {
      var productIds = traceabilityService.getProductsForAnimal(request.getRegistrationNumber());

      var response = GetProductsForAnimalResponse.newBuilder().addAllProductIds(productIds).build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }
    catch (NoSuchElementException e)
    {
      responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    }
    catch (RuntimeException e)
    {
      responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }
}
