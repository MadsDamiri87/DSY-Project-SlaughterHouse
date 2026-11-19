package com.example.traceability.messaging;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration public class RabbitConfig
{
  private final String exchange;
  private final String deadLetterExchange;

  public RabbitConfig(@Value("${app.events.exchange}") String exchange,
                      @Value("${app.events.dead-letter-exchange}") String deadLetterExchange)
  {
    this.exchange = exchange;
    this.deadLetterExchange = deadLetterExchange;
  }
  public static final String ANIMAL_QUEUE = "traceability.animal";
  public static final String TRAY_QUEUE = "traceability.tray";
  public static final String PART_QUEUE = "traceability.part";
  public static final String PRODUCT_QUEUE = "traceability.product";
  public static final String DEAD_LETTER_QUEUE = "traceability.dead-letter";

  @Bean public TopicExchange eventsExchange()
  {
    return new TopicExchange(exchange, true, false);
  }

  @Bean public TopicExchange deadLetterExchage()
  {
    return new TopicExchange(deadLetterExchange, true, false);
  }

  @Bean public Queue deadLetterQueue()
  {
    return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
  }

  @Bean public Binding deadLetterBinding()
  {
    return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchage()).with("#");
  }

  @Bean public Queue animalQueue()
  {
    return eventQueue(ANIMAL_QUEUE);
  }

  @Bean public Queue trayQueue()
  {
    return eventQueue(TRAY_QUEUE);
  }

  @Bean public Queue partQueue()
  {
    return eventQueue(PART_QUEUE);
  }

  @Bean public Queue productQueue()
  {
    return eventQueue(PRODUCT_QUEUE);
  }

  @Bean public Binding animalBinding()
  {
    return BindingBuilder.bind(animalQueue()).to(eventsExchange()).with("animal.#");
  }

  @Bean public Binding trayBinding()
  {
    return BindingBuilder.bind(trayQueue()).to(eventsExchange()).with("tray.#");
  }

  @Bean public Binding partBinding()
  {
    return BindingBuilder.bind(partQueue()).to(eventsExchange()).with("part.#");
  }

  @Bean public Binding productBinding()
  {
    return BindingBuilder.bind(productQueue()).to(eventsExchange()).with("product.#");
  }

  private Queue eventQueue(String name)
  {
    return QueueBuilder.durable(name).deadLetterExchange(deadLetterExchange).build();
  }

}
