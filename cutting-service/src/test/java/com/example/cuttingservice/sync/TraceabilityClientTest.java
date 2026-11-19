package com.example.cuttingservice.sync;

import com.example.cuttingservice.outbox.OutboxEntry;
import com.example.cuttingservice.outbox.OutboxEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/*
 * Enhedstest af transporten ud af Station 2.
 *
 * RabbitTemplate er mocket. Station 2 er den eneste med tre hændelsestyper, så det
 * der testes her er routingnøglerne — rammer de forkert, havner delen i den forkerte kø.
 */
@ExtendWith(MockitoExtension.class)
class TraceabilityClientTest
{
  private static final String EXCHANGE = "slaughterhouse.events";

  @Mock private RabbitTemplate rabbitTemplate;

  private TraceabilityClient client;

  @BeforeEach
  void createClient()
  {
    client = new TraceabilityClient(rabbitTemplate, EXCHANGE);
  }

  /*
   * De to delhændelser deler præfiks med vilje. Serverens binding er part.#, så de
   * ender i samme kø og dermed i rækkefølge — en del bliver registreret før den lægges
   * i en bakke. Bakken har sin egen kø, for der er rækkefølgen ligegyldig.
   */
  @Test
  void eachEventTypeGetsItsOwnRoutingKey()
  {
    brokerAcknowledges();
    ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);

    client.send(OutboxEntry.of(OutboxEventType.TRAY_CREATED, "{}"));
    client.send(OutboxEntry.of(OutboxEventType.PART_REGISTERED, "{}"));
    client.send(OutboxEntry.of(OutboxEventType.PART_PLACED_IN_TRAY, "{}"));

    verify(rabbitTemplate, times(3))
        .convertAndSend(eq(EXCHANGE), routingKey.capture(), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

    assertThat(routingKey.getAllValues())
        .containsExactly("tray.created", "part.registered", "part.placed");
  }

  // Payloaden sendes videre som den ligger i outboxen, uden at blive pakket om.
  @Test
  void theOutboxPayloadIsSentUnchanged()
  {
    brokerAcknowledges();
    ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);

    client.send(OutboxEntry.of(OutboxEventType.PART_REGISTERED, "{\"partId\":\"PART-00001\"}"));

    verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), any(String.class), payload.capture(),
                                          any(MessagePostProcessor.class),
                                          any(CorrelationData.class));
    assertThat(payload.getValue()).isEqualTo("{\"partId\":\"PART-00001\"}");
  }

  // Nåede beskeden ingen kø, skal hændelsen blive liggende — ikke slettes som leveret.
  @Test
  void anEventThatReachedNoQueueIsReportedAsUnavailable()
  {
    doAnswer(call ->
    {
      CorrelationData correlation = call.getArgument(4);
      correlation.setReturned(new ReturnedMessage(
          new Message(new byte[0], new MessageProperties()), 312, "NO_ROUTE", EXCHANGE,
          "part.registered"));
      correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
      return null;
    }).when(rabbitTemplate).convertAndSend(eq(EXCHANGE), any(String.class), any(Object.class),
                                           any(MessagePostProcessor.class),
                                           any(CorrelationData.class));

    assertThatThrownBy(() -> client.send(OutboxEntry.of(OutboxEventType.PART_REGISTERED, "{}")))
        .isInstanceOf(BackendUnavailableException.class);
  }

  private void brokerAcknowledges()
  {
    doAnswer(call ->
    {
      CorrelationData correlation = call.getArgument(4);
      correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
      return null;
    }).when(rabbitTemplate).convertAndSend(eq(EXCHANGE), any(String.class), any(Object.class),
                                           any(MessagePostProcessor.class),
                                           any(CorrelationData.class));
  }
}
