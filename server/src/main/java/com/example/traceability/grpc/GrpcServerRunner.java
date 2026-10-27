package com.example.traceability.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@ConditionalOnProperty(name = "grpc.server.enabled", matchIfMissing = true)
public class GrpcServerRunner implements CommandLineRunner
{
  private static final Logger log = LoggerFactory.getLogger(GrpcServerRunner.class);

  private final TraceabilityGrpcService grpcService;
  private final int port;

  public GrpcServerRunner(TraceabilityGrpcService grpcService, @Value("${grpc.server.port:9090}") int port)
  {
    this.grpcService = grpcService;
    this.port = port;
  }

  @Override
  public void run(String... args) throws Exception
  {
    Server server = ServerBuilder
        .forPort(port)
        .addService(grpcService)
        .addService(ProtoReflectionService.newInstance())
        .build()
        .start();

    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

    log.info("gRPC server started on port {}", port);

    server.awaitTermination();
  }
}
