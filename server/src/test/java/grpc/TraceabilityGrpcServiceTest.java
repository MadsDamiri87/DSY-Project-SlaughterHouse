package grpc;

import generated.GetAnimalsForProductRequest;
import generated.GetProductsForAnimalRequest;
import generated.TraceabilityServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.TraceabilityService;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test af gRPC-laget: oversætter det korrekt mellem domænet og protokollen?
 *
 * Vi starter en rigtig gRPC-server, men "in-process" — den kører i samme JVM uden
 * netværk eller portnummer. Vi tester altså rigtige gRPC-kald (serialisering, statuskoder),
 * bare uden at være afhængige af at en port er ledig.
 *
 * TraceabilityService er mocket, fordi vi her kun vil teste laget udenom den.
 */
class TraceabilityGrpcServiceTest
{
  private TraceabilityService traceabilityService;
  private Server server;
  private ManagedChannel channel;
  private TraceabilityServiceGrpc.TraceabilityServiceBlockingStub stub;

  // Kører før hver test: ny server, ny mock, ny kanal — så testene ikke påvirker hinanden.
  @BeforeEach
  void startInProcessServer() throws IOException
  {
    traceabilityService = mock(TraceabilityService.class);

    // Et unikt navn i stedet for en port. directExecutor() kører kaldene på testtråden,
    // hvilket gør testen deterministisk i stedet for afhængig af timing.
    String serverName = InProcessServerBuilder.generateName();

    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TraceabilityGrpcService(traceabilityService))
        .build()
        .start();

    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    stub = TraceabilityServiceGrpc.newBlockingStub(channel);
  }

  // Rydder op, så vi ikke efterlader tråde der hober sig op gennem testkørslen.
  @AfterEach
  void stopInProcessServer() throws InterruptedException
  {
    channel.shutdownNow();
    server.shutdownNow();
    channel.awaitTermination(5, TimeUnit.SECONDS);
    server.awaitTermination(5, TimeUnit.SECONDS);
  }

  // Det glade scenarie: servicens liste skal ende i responsens repeated-felt.
  @Test
  void getAnimalsForProductReturnsTheAnimalIdsFromTheService()
  {
    when(traceabilityService.getAnimalsForProduct("PROD-1")).thenReturn(List.of(1, 2));

    var response = stub.getAnimalsForProduct(
        GetAnimalsForProductRequest.newBuilder().setProductId("PROD-1").build());

    assertThat(response.getAnimalIdsList()).containsExactly(1, 2);
  }

  // Samme kontrol den anden vej.
  @Test
  void getProductsForAnimalReturnsTheProductIdsFromTheService()
  {
    when(traceabilityService.getProductsForAnimal(1)).thenReturn(List.of("PROD-1", "PROD-2"));

    var response = stub.getProductsForAnimal(
        GetProductsForAnimalRequest.newBuilder().setAnimalId(1).build());

    assertThat(response.getProductIdsList()).containsExactly("PROD-1", "PROD-2");
  }

  /*
   * Kernen i denne testklasse: servicen kaster NoSuchElementException,
   * og klienten skal se statuskoden NOT_FOUND — ikke en ubrugelig UNKNOWN.
   */
  @Test
  void unknownProductIsReportedAsNotFound()
  {
    when(traceabilityService.getAnimalsForProduct("MISSING"))
        .thenThrow(new NoSuchElementException("No product with id MISSING"));

    assertThatThrownBy(() -> stub.getAnimalsForProduct(
        GetAnimalsForProductRequest.newBuilder().setProductId("MISSING").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
        .isEqualTo(Status.Code.NOT_FOUND);
  }

  // Samme oversættelse for det ukendte dyr.
  @Test
  void unknownAnimalIsReportedAsNotFound()
  {
    when(traceabilityService.getProductsForAnimal(99))
        .thenThrow(new NoSuchElementException("No animal with id 99"));

    assertThatThrownBy(() -> stub.getProductsForAnimal(
        GetProductsForAnimalRequest.newBuilder().setAnimalId(99).build()))
        .isInstanceOf(StatusRuntimeException.class)
        .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
        .isEqualTo(Status.Code.NOT_FOUND);
  }

  /*
   * Tomt input er klientens fejl, ikke serverens. Derfor INVALID_ARGUMENT,
   * og servicen bliver slet ikke kaldt.
   */
  @Test
  void blankProductIdIsRejectedAsInvalidArgument()
  {
    assertThatThrownBy(() -> stub.getAnimalsForProduct(
        GetAnimalsForProductRequest.newBuilder().setProductId("  ").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
        .isEqualTo(Status.Code.INVALID_ARGUMENT);
  }

  // I proto3 er et manglende int-felt lig 0, så 0 betyder reelt "intet id angivet".
  @Test
  void nonPositiveAnimalIdIsRejectedAsInvalidArgument()
  {
    assertThatThrownBy(() -> stub.getProductsForAnimal(
        GetProductsForAnimalRequest.newBuilder().setAnimalId(0).build()))
        .isInstanceOf(StatusRuntimeException.class)
        .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
        .isEqualTo(Status.Code.INVALID_ARGUMENT);
  }

  // Uventede fejl (fx databasen er nede) må ikke slippe ud som UNKNOWN,
  // men skal meldes som INTERNAL, så klienten kan skelne dem fra NOT_FOUND.
  @Test
  void unexpectedServiceFailureIsReportedAsInternal()
  {
    when(traceabilityService.getAnimalsForProduct("PROD-1"))
        .thenThrow(new IllegalStateException("database unavailable"));

    assertThatThrownBy(() -> stub.getAnimalsForProduct(
        GetAnimalsForProductRequest.newBuilder().setProductId("PROD-1").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
        .isEqualTo(Status.Code.INTERNAL);
  }
}
