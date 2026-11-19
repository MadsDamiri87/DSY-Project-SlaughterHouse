package com.example.registration.sync;

import com.example.registration.outbox.OutboxEntry;
import com.example.registration.outbox.OutboxEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/*
 * Enhedstest af transporten ud af stationen.
 *
 * RabbitTemplate er mocket, så der hverken er broker eller netværk med. Det der testes
 * er hvornår klienten siger "prøv igen senere" til OutboxPublisher — for siger den det
 * ikke, bliver hændelsen slettet fra outboxen.
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

  // Et registreret dyr skal ud under sin egen routingnøgle, så det rammer dyrekøen.
  @Test
  void aRegisteredAnimalIsPublishedUnderItsOwnRoutingKey()
  {
    brokerReplies(true, false);
    ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);

    client.send(OutboxEntry.of(OutboxEventType.ANIMAL_REGISTERED, "{}"));

    verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), routingKey.capture(), any(Object.class),
                                          any(MessagePostProcessor.class),
                                          any(CorrelationData.class));
    assertThat(routingKey.getValue()).isEqualTo("animal.registered");
  }

  // Kvitterer brokeren, er hændelsen afleveret, og publisheren må slette den.
  @Test
  void anAcknowledgedEventIsAccepted()
  {
    brokerReplies(true, false);

    assertThatCode(() -> client.send(OutboxEntry.of(OutboxEventType.ANIMAL_REGISTERED, "{}")))
        .doesNotThrowAnyException();
  }

  // Afviser brokeren beskeden, skal hændelsen blive liggende og prøves igen.
  @Test
  void aNegativeAcknowledgementIsReportedAsUnavailable()
  {
    brokerReplies(false, false);

    assertThatThrownBy(() -> client.send(OutboxEntry.of(OutboxEventType.ANIMAL_REGISTERED, "{}")))
        .isInstanceOf(BackendUnavailableException.class);
  }

  /*
   * Den vigtigste af dem alle. Findes køerne ikke endnu, tager brokeren imod beskeden,
   * kvitterer for den og smider den væk. Kvitteringen alene er derfor ikke nok —
   * uden tjekket på den returnerede besked ville hændelsen blive slettet og tabt.
   */
  @Test
  void anEventThatReachedNoQueueIsReportedAsUnavailable()
  {
    brokerReplies(true, true);

    assertThatThrownBy(() -> client.send(OutboxEntry.of(OutboxEventType.ANIMAL_REGISTERED, "{}")))
        .isInstanceOf(BackendUnavailableException.class);
  }

  // En broker der slet ikke svarer er også "prøv igen senere", ikke en permanent fejl.
  @Test
  void aBrokerThatCannotBeReachedIsReportedAsUnavailable()
  {
    doThrow(new AmqpException("brokeren er nede"))
        .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), any(String.class), any(Object.class),
                                             any(MessagePostProcessor.class),
                                             any(CorrelationData.class));

    assertThatThrownBy(() -> client.send(OutboxEntry.of(OutboxEventType.ANIMAL_REGISTERED, "{}")))
        .isInstanceOf(BackendUnavailableException.class);
  }

  /*
   * Erstatter brokeren: fuldfører den future klienten venter på, og lægger eventuelt
   * en returneret besked på, som en rigtig broker gør når intet kan modtage beskeden.
   */
  private void brokerReplies(boolean acknowledged, boolean returnedToSender)
  {
    doAnswer(call ->
    {
      CorrelationData correlation = call.getArgument(4);

      if (returnedToSender)
      {
        correlation.setReturned(new ReturnedMessage(
            new Message(new byte[0], new MessageProperties()), 312, "NO_ROUTE", EXCHANGE,
            "animal.registered"));
      }

      correlation.getFuture()
                 .complete(new CorrelationData.Confirm(acknowledged,
                                                       acknowledged ? null : "afvist"));
      return null;
    }).when(rabbitTemplate).convertAndSend(eq(EXCHANGE), any(String.class), any(Object.class),
                                           any(MessagePostProcessor.class),
                                           any(CorrelationData.class));
  }
}
