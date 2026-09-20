package grpc;

import generated.GetAnimalsForProductRequest;
import generated.GetAnimalsForProductResponse;
import generated.GetProductsForAnimalRequest;
import generated.GetProductsForAnimalResponse;
import generated.TraceabilityServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import service.TraceabilityService;

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
      var animalIds = traceabilityService.getAnimalsForProduct(request.getProductId());

      var response = GetAnimalsForProductResponse.newBuilder().addAllAnimalIds(animalIds).build();

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

  @Override public void getProductsForAnimal(GetProductsForAnimalRequest request,
                                             StreamObserver<GetProductsForAnimalResponse> responseObserver)
  {
    if (request.getAnimalId() <= 0)
    {
      responseObserver.onError(Status.INVALID_ARGUMENT
          .withDescription("animalId must be a positive number")
          .asRuntimeException());
      return;
    }

    try
    {
      var productIds = traceabilityService.getProductsForAnimal(request.getAnimalId());

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
