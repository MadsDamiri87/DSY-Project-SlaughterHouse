package com.example.cuttingservice.sync;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration public class RabbitConfig
{
  @Bean public TopicExchange eventsExchange(@Value("${app.events.exchange}") String exchange)
  {
    return new TopicExchange(exchange, true, false);
  }

  @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory)
  {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMandatory(true);
    return template;
  }
}
