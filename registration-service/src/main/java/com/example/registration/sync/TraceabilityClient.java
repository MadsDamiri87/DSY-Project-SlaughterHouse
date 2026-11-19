package com.example.registration.sync;

import com.example.registration.outbox.OutboxEntry;
import com.example.registration.outbox.OutboxEventType;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component public class TraceabilityClient
{
  private static final long CONFIRM_TIMEOUT_SECONDS = 5;

  private final RabbitTemplate rabbitTemplate;
  private final String exchange;

  public TraceabilityClient(RabbitTemplate rabbitTemplate,
                            @Value("${app.events.exchange}") String exchange)
  {
    this.rabbitTemplate = rabbitTemplate;
    this.exchange       = exchange;
  }

  public void send(OutboxEntry entry)
  {
    CorrelationData correlation = new CorrelationData(entry.getEventId().toString());

    try
    {
      rabbitTemplate.convertAndSend(exchange, routingKeyFor(entry.getType()), entry.getPayload(),
                                    message -> {
                                      message.getMessageProperties()
                                             .setContentType(MessageProperties.CONTENT_TYPE_JSON);
                                      message.getMessageProperties()
                                             .setMessageId(entry.getEventId().toString());
                                      return message;
                                    }, correlation);

      CorrelationData.Confirm confirm = correlation.getFuture()
                                                   .get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (confirm == null || !confirm.isAck() || correlation.getReturned() != null)
      {
        throw new BackendUnavailableException(
            "The broker did not acknowledge event " + entry.getEventId());
      }
    }
    catch (AmqpException | ExecutionException | TimeoutException exception)
    {
      throw new BackendUnavailableException(
          "The broker could not be reached for event " + entry.getEventId(), exception);
    }
    catch (InterruptedException e)
    {
      Thread.currentThread().interrupt();
      throw new BackendUnavailableException("Interrupted while waiting for confirmation " + e);
    }
  }

  private String routingKeyFor(OutboxEventType type)
  {
    return switch (type)
    {
      case ANIMAL_REGISTERED -> "animal.registered";
    };
  }

}
