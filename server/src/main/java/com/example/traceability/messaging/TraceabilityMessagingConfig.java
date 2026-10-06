package com.example.traceability.messaging;

import com.example.shared.messaging.MessagingTopology;
import com.example.shared.messaging.SharedMessagingConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SharedMessagingConfig.class)
public class TraceabilityMessagingConfig
{
  @Bean
  TopicExchange animalDeadLetterExchange()
  {
    return new TopicExchange(MessagingTopology.DEAD_LETTER_EXCHANGE, true, false);
  }

  @Bean
  Queue animalRegistrationDeadLetterQueue()
  {
    return QueueBuilder.durable(MessagingTopology.ANIMAL_REGISTRATION_DEAD_LETTER_QUEUE).build();
  }

  @Bean
  Binding animalRegistrationDeadLetterBinding(Queue animalRegistrationDeadLetterQueue,
                                              TopicExchange animalDeadLetterExchange)
  {
    return BindingBuilder.bind(animalRegistrationDeadLetterQueue)
        .to(animalDeadLetterExchange)
        .with(MessagingTopology.ANIMAL_REGISTERED_ROUTING_KEY);
  }

  @Bean
  Queue animalRegistrationQueue()
  {
    return QueueBuilder.durable(MessagingTopology.ANIMAL_REGISTRATION_QUEUE)
        .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
        .deadLetterRoutingKey(MessagingTopology.ANIMAL_REGISTERED_ROUTING_KEY)
        .build();
  }

  @Bean
  Binding animalRegistrationBinding(Queue animalRegistrationQueue,
                                    TopicExchange animalExchange)
  {
    return BindingBuilder.bind(animalRegistrationQueue)
        .to(animalExchange)
        .with(MessagingTopology.ANIMAL_REGISTERED_ROUTING_KEY);
  }
}
