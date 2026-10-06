package com.example.registration.messaging;

import com.example.registration.entity.Animal;
import com.example.registration.entity.AnimalRegistrationOutbox;
import com.example.registration.repository.AnimalRegistrationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimalRegistrationOutboxPublisherTest
{
  private final AnimalRegistrationOutboxRepository outboxRepository =
      mock(AnimalRegistrationOutboxRepository.class);
  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final AnimalRegistrationOutboxPublisher publisher =
      new AnimalRegistrationOutboxPublisher(outboxRepository, rabbitTemplate);

  @Test
  void removesEventOnlyAfterItHasBeenSent()
  {
    AnimalRegistrationOutbox pending = pendingEvent();
    when(outboxRepository.findTop100ByOrderByOccurredAtAsc()).thenReturn(List.of(pending));

    publisher.publishPending();

    verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    verify(outboxRepository).delete(pending);
  }

  @Test
  void keepsEventWhenRabbitMqIsUnavailable()
  {
    AnimalRegistrationOutbox pending = pendingEvent();
    when(outboxRepository.findTop100ByOrderByOccurredAtAsc()).thenReturn(List.of(pending));
    org.mockito.Mockito.doThrow(new AmqpConnectException(new RuntimeException("offline")))
        .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

    publisher.publishPending();

    verify(outboxRepository, never()).delete(any());
  }

  private AnimalRegistrationOutbox pendingEvent()
  {
    return AnimalRegistrationOutbox.from(
        new Animal(42, LocalDateTime.of(2026, 10, 6, 8, 30), 95.5, "Nørregaard"));
  }
}
