package com.example.traceability.messaging;

import com.example.shared.messaging.AnimalRegisteredEvent;
import com.example.shared.messaging.MessagingTopology;
import com.example.traceability.entity.Animal;
import com.example.traceability.repository.AnimalRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AnimalRegisteredConsumer
{
  private final AnimalRepository animalRepository;

  public AnimalRegisteredConsumer(AnimalRepository animalRepository)
  {
    this.animalRepository = animalRepository;
  }

  @RabbitListener(queues = MessagingTopology.ANIMAL_REGISTRATION_QUEUE)
  @Transactional
  public void consume(AnimalRegisteredEvent event)
  {
    if (event.registrationNumber() <= 0)
    {
      throw new IllegalArgumentException("Registration number must be positive");
    }

    Animal animal = animalRepository.findById(event.registrationNumber())
        .orElseGet(() -> new Animal(event.registrationNumber(), event.weight()));

    animal.setWeight(event.weight());
    animal.setArrivalDateTime(event.arrivalDateTime());
    animal.setOrigin(event.origin());
    animalRepository.save(animal);
  }
}
