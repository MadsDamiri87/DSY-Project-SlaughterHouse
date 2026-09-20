package grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class GrpcServerRunner implements CommandLineRunner
{
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

    System.out.println("gRPC server started on port " + port);

    server.awaitTermination();
  }
}
