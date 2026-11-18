package com.example.desktop.api;

import generated.GetAnimalTraceRequest;
import generated.GetAnimalTraceResponse;
import generated.GetAnimalsForProductRequest;
import generated.GetPartTraceRequest;
import generated.GetPartTraceResponse;
import generated.TraceabilityServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TraceApi implements AutoCloseable
{
  private final ManagedChannel channel;
  private final TraceabilityServiceGrpc.TraceabilityServiceBlockingStub stub;

  public TraceApi(String host, int port)
  {
    this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    this.stub = TraceabilityServiceGrpc.newBlockingStub(channel);
  }

  public GetAnimalTraceResponse animalTrace(int registrationNumber)
  {
    return call(() -> stub.withDeadlineAfter(5, TimeUnit.SECONDS)
        .getAnimalTrace(GetAnimalTraceRequest.newBuilder()
            .setRegistrationNumber(registrationNumber)
            .build()));
  }

  public GetPartTraceResponse partTrace(String partId)
  {
    return call(() -> stub.withDeadlineAfter(5, TimeUnit.SECONDS)
        .getPartTrace(GetPartTraceRequest.newBuilder()
            .setPartId(partId)
            .build()));
  }

  public List<Integer> animalsForProduct(String productId)
  {
    return call(() -> stub.withDeadlineAfter(5, TimeUnit.SECONDS)
        .getAnimalsForProduct(GetAnimalsForProductRequest.newBuilder()
            .setProductId(productId)
            .build())).getRegistrationNumbersList();
  }

  @Override
  public void close()
  {
    channel.shutdown();

    try
    {
      channel.awaitTermination(5, TimeUnit.SECONDS);
    }
    catch (InterruptedException exception)
    {
      Thread.currentThread().interrupt();
    }
  }

  private <T> T call(Call<T> call)
  {
    try
    {
      return call.execute();
    }
    catch (StatusRuntimeException exception)
    {
      throw switch (exception.getStatus().getCode())
      {
        case UNAVAILABLE, DEADLINE_EXCEEDED -> new BackendUnavailableException(
            "Sporingsserveren svarer ikke", exception);
        case NOT_FOUND -> new ApiException(404, describe(exception));
        case INVALID_ARGUMENT -> new ApiException(400, describe(exception));
        default -> new ApiException(500, describe(exception));
      };
    }
  }

  private String describe(StatusRuntimeException exception)
  {
    String description = exception.getStatus().getDescription();
    return description == null || description.isBlank()
        ? exception.getStatus().getCode().name()
        : description;
  }

  private interface Call<T>
  {
    T execute();
  }
}
