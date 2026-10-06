package com.example.registration.messaging;

import com.example.registration.repository.AnimalRegistrationOutboxRepository;
import com.example.shared.messaging.MessagingTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", matchIfMissing = true)
public class AnimalRegistrationOutboxPublisher
{
  private static final Logger log =
      LoggerFactory.getLogger(AnimalRegistrationOutboxPublisher.class);

  private final AnimalRegistrationOutboxRepository outboxRepository;
  private final RabbitTemplate rabbitTemplate;

  public AnimalRegistrationOutboxPublisher(AnimalRegistrationOutboxRepository outboxRepository,
                                           RabbitTemplate rabbitTemplate)
  {
    this.outboxRepository = outboxRepository;
    this.rabbitTemplate = rabbitTemplate;
  }

  @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:5000}")
  @Transactional
  public void publishPending()
  {
    for (var pending : outboxRepository.findTop100ByOrderByOccurredAtAsc())
    {
      try
      {
        rabbitTemplate.convertAndSend(MessagingTopology.ANIMAL_EXCHANGE,
            MessagingTopology.ANIMAL_REGISTERED_ROUTING_KEY, pending.toEvent());
        outboxRepository.delete(pending);
      }
      catch (AmqpException exception)
      {
        log.warn("Animal registration event {} is still waiting for RabbitMQ",
            pending.getEventId());
        break;
      }
    }
  }
}
