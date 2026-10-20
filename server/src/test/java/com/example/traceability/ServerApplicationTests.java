package com.example.traceability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/*
 * Beviser at hele applikationen kan starte: alle beans bliver fundet, injiceret
 * og sat sammen, og JPA finder sine entiteter og repositories.
 *
 * Fejler denne test, starter serveren heller ikke i virkeligheden - og det er
 * den slags fejl de andre tests ikke fanger, fordi de kun samler dele af systemet.
 *
 * grpc.server.enabled=false slaar GrpcServerRunner fra under testen. Den kalder
 * server.awaitTermination(), som blokerer for evigt, og uden flaget ville testen
 * derfor haenge i stedet for at slutte.
 */
@SpringBootTest(properties = "grpc.server.enabled=false")
class ServerApplicationTests
{
  @Test
  void contextLoads()
  {
  }
}
