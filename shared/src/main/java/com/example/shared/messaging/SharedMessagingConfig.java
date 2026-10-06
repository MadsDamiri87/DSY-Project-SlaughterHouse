package com.example.shared.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SharedMessagingConfig
{
  @Bean
  public TopicExchange animalExchange()
  {
    return new TopicExchange(MessagingTopology.ANIMAL_EXCHANGE, true, false);
  }

  @Bean
  public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper)
  {
    return new Jackson2JsonMessageConverter(objectMapper);
  }
}
