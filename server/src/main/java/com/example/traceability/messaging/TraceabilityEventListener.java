package com.example.traceability.messaging;

import com.example.traceability.dto.AnimalDTO;
import com.example.traceability.dto.AnimalPartDTO;
import com.example.traceability.dto.ProductDTO;
import com.example.traceability.dto.TrayDTO;
import com.example.traceability.service.IngestionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TraceabilityEventListener
{
  private final IngestionService ingestionService;
  private final ObjectMapper objectMapper;
  private final Validator validator;

  public TraceabilityEventListener(IngestionService ingestionService, ObjectMapper objectMapper,
                                   Validator validator)
  {
    this.ingestionService = ingestionService;
    this.objectMapper     = objectMapper;
    this.validator        = validator;
  }

  @RabbitListener(queues = RabbitConfig.ANIMAL_QUEUE)
  public void onAnimal(String payload)
  {
    ingestionService.receiveAnimal(read(payload, AnimalDTO.class));
  }

  @RabbitListener(queues = RabbitConfig.TRAY_QUEUE)
  public void onTray(String payload)
  {
    ingestionService.receiveTray(read(payload, TrayDTO.class));
  }

  @RabbitListener(queues = RabbitConfig.PART_QUEUE)
  public void onPart(String payload)
  {
    ingestionService.receivePart(read(payload, AnimalPartDTO.class));
  }

  @RabbitListener(queues = RabbitConfig.PRODUCT_QUEUE)
  public void onProduct(String payload)
  {
    ingestionService.receiveProduct(read(payload, ProductDTO.class));
  }

  private <T> T read(String payload, Class<T> type)
  {
    T value;

    try
    {
      value = objectMapper.readValue(payload, type);
    }
    catch (JsonProcessingException e)
    {
      throw new AmqpRejectAndDontRequeueException("Unreadable " + type.getSimpleName() + " payload",
                                                  e);
    }

    Set<ConstraintViolation<T>> violations = validator.validate(value);

    if (!violations.isEmpty())
    {
      String detail = violations.stream()
          .map(v -> v.getPropertyPath() + " " + v.getMessage())
          .sorted()
          .collect(Collectors.joining(", "));

      throw new AmqpRejectAndDontRequeueException(
          "Invalid " + type.getSimpleName() + ": " + detail);
    }

    return value;
  }

}
