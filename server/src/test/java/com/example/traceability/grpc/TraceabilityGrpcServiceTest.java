package com.example.traceability.grpc;

import generated.GetAnimalTraceRequest;
import generated.GetAnimalsForProductRequest;
import generated.GetPartTraceRequest;
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
import com.example.traceability.service.AnimalTrace;
import com.example.traceability.service.PartTrace;
import com.example.traceability.service.TraceabilityService;
import com.example.traceability.service.TracedPart;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test af gRPC-laget: korrekt oversættelse mellem domænet og protokollen?
 *
 * Starter en rigtig gRPC-server, men "in-process" — den kører i samme JVM uden
 * netværk eller portnummer. Vi tester altså rigtige gRPC-kald (serialisering, statuskoder),
 * bare uden at være afhængige af at en port er ledig.
 *
 * TraceabilityService er mocket, fordi vi her kun vil teste laget udenom den.
 */
class TraceabilityGrpcServiceTest
{
  private static final Instant CUT_AT = Instant.parse("2026-10-27T08:30:00Z");

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

  // Happy case scenario: servicens liste skal ende i responsens repeated-felt.
  @Test
  void getAnimalsForProductReturnsTheRegistrationNumbersFromTheService()
  {
    when(traceabilityService.getAnimalsForProduct("PROD-1")).thenReturn(List.of(1, 2));

    var response = stub.getAnimalsForProduct(
        GetAnimalsForProductRequest.newBuilder().setProductId("PROD-1").build());

    assertThat(response.getRegistrationNumbersList()).containsExactly(1, 2);
  }

  // Samme kontrol den anden vej.
  @Test
  void getProductsForAnimalReturnsTheProductIdsFromTheService()
  {
    when(traceabilityService.getProductsForAnimal(1)).thenReturn(List.of("PROD-1", "PROD-2"));

    var response = stub.getProductsForAnimal(
        GetProductsForAnimalRequest.newBuilder().setRegistrationNumber(1).build());

    assertThat(response.getProductIdsList()).containsExactly("PROD-1", "PROD-2");
  }

  /*
   * Den fulde sporing skal kunne rejse over protokollen med alle mellemleddene i behold:
   * delens bakke og de produkter bakken endte i, ikke bare en flad liste af produkt-id'er.
   */
  @Test
  void getAnimalTraceCarriesEveryPartWithItsTrayAndProducts()
  {
    TracedPart loin = new TracedPart("PART-1", "LOIN", 6.2, CUT_AT, "TRAY-LOIN-1",
        List.of("PROD-LOIN-PACK", "PROD-HALF-ANIMAL"));

    when(traceabilityService.traceAnimal(1)).thenReturn(
        new AnimalTrace(1, "Pig", "Noerregaard", 95.0, CUT_AT, List.of(loin)));

    var response = stub.getAnimalTrace(
        GetAnimalTraceRequest.newBuilder().setRegistrationNumber(1).build());

    assertThat(response.getSpecies()).isEqualTo("Pig");
    assertThat(response.getPartsCount()).isEqualTo(1);
    assertThat(response.getParts(0).getTrayId()).isEqualTo("TRAY-LOIN-1");
    assertThat(response.getParts(0).getProductIdsList())
        .containsExactly("PROD-LOIN-PACK", "PROD-HALF-ANIMAL");
  }

  /*
   * Protobuf har ingen null. En del der ikke er lagt i en bakke endnu, og et dyr
   * uden art, skal derfor komme over som tomme tekster og ikke få kaldet til at fejle.
   */
  @Test
  void missingValuesAreSentAsEmptyTextAndNotAsAFailure()
  {
    TracedPart loose = new TracedPart("PART-5", "SHOULDER", 5.0, null, null, List.of());

    when(traceabilityService.traceAnimal(2)).thenReturn(
        new AnimalTrace(2, null, null, 0.0, null, List.of(loose)));

    var response = stub.getAnimalTrace(
        GetAnimalTraceRequest.newBuilder().setRegistrationNumber(2).build());

    assertThat(response.getSpecies()).isEmpty();
    assertThat(response.getArrivalDateTime()).isEmpty();
    assertThat(response.getParts(0).getTrayId()).isEmpty();
    assertThat(response.getParts(0).getProductIdsList()).isEmpty();
  }

  // Den modsatte vej: fra én del tilbage til dyret den kom fra.
  @Test
  void getPartTraceCarriesTheAnimalThePartCameFrom()
  {
    TracedPart belly = new TracedPart("PART-3", "BELLY", 4.5, CUT_AT, "TRAY-BELLY-1",
        List.of("PROD-HALF-ANIMAL"));

    when(traceabilityService.tracePart("PART-3"))
        .thenReturn(new PartTrace(belly, 1, "Pig", "Noerregaard"));

    var response = stub.getPartTrace(
        GetPartTraceRequest.newBuilder().setPartId("PART-3").build());

    assertThat(response.getRegistrationNumber()).isEqualTo(1);
    assertThat(response.getPart().getType()).isEqualTo("BELLY");
    assertThat(response.getPart().getProductIdsList()).containsExactly("PROD-HALF-ANIMAL");
  }

  @Test
  void unknownPartIsReportedAsNotFound()
  {
    when(traceabilityService.tracePart("MISSING"))
        .thenThrow(new NoSuchElementException("No animal part with id MISSING"));

    assertThatThrownBy(() -> stub.getPartTrace(
        GetPartTraceRequest.newBuilder().setPartId("MISSING").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
        .isEqualTo(Status.Code.NOT_FOUND);
  }

  // Tomt del-id er klientens fejl, og servicen skal slet ikke kaldes.
  @Test
  void aBlankPartIdIsRejectedBeforeTheServiceIsCalled()
  {
    assertThatThrownBy(() -> stub.getPartTrace(
        GetPartTraceRequest.newBuilder().setPartId("  ").build()))
        .isInstanceOf(StatusRuntimeException.class)
        .extracting(e -> ((StatusRuntimeException) e).getStatus().getCode())
        .isEqualTo(Status.Code.INVALID_ARGUMENT);

    verifyNoInteractions(traceabilityService);
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
        GetProductsForAnimalRequest.newBuilder().setRegistrationNumber(99).build()))
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
  void nonPositiveRegistrationNumberIsRejectedAsInvalidArgument()
  {
    assertThatThrownBy(() -> stub.getProductsForAnimal(
        GetProductsForAnimalRequest.newBuilder().setRegistrationNumber(0).build()))
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
