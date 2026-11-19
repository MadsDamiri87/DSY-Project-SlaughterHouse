package com.example.packing.sync;

import com.example.packing.outbox.OutboxEntry;
import com.example.packing.outbox.OutboxEventType;
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

/*
 * Enhedstest af transporten ud af Station 3.
 *
 * RabbitTemplate er mocket. Station 3 har kun én hændelsestype, så der testes
 * routingnøglen og den ene fejl der ellers ville koste hændelsen.
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

  // Et pakket produkt skal ud under product.packed, så det rammer produktkøen.
  @Test
  void aPackedProductIsPublishedUnderItsOwnRoutingKey()
  {
    doAnswer(call ->
    {
      CorrelationData correlation = call.getArgument(4);
      correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
      return null;
    }).when(rabbitTemplate).convertAndSend(eq(EXCHANGE), any(String.class), any(Object.class),
                                           any(MessagePostProcessor.class),
                                           any(CorrelationData.class));

    ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);

    client.send(OutboxEntry.of(OutboxEventType.PRODUCT_PACKED, "{}"));

    verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), routingKey.capture(), any(Object.class),
                                          any(MessagePostProcessor.class),
                                          any(CorrelationData.class));
    assertThat(routingKey.getValue()).isEqualTo("product.packed");
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
          "product.packed"));
      correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
      return null;
    }).when(rabbitTemplate).convertAndSend(eq(EXCHANGE), any(String.class), any(Object.class),
                                           any(MessagePostProcessor.class),
                                           any(CorrelationData.class));

    assertThatThrownBy(() -> client.send(OutboxEntry.of(OutboxEventType.PRODUCT_PACKED, "{}")))
        .isInstanceOf(BackendUnavailableException.class);
  }
}
