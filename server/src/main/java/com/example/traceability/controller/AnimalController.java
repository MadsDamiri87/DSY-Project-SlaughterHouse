package com.example.traceability.controller;

import com.example.traceability.dto.AnimalDTO;
import com.example.traceability.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Kaldes ikke længere af registration-service. Stationens outbox sender nu
// ANIMAL_REGISTERED over AMQP med routingnøglen animal.registered, som
// serverens lytter på køen traceability.animal modtager.
// Endepunktet er bevaret, så modtagelsen også kan afprøves med Postman.

@RestController
@RequestMapping("/animals")
public class AnimalController
{
  private final IngestionService ingestionService;

  public AnimalController(IngestionService ingestionService)
  {
    this.ingestionService = ingestionService;
  }

  @PostMapping
  public ResponseEntity<Void> receive(@Valid @RequestBody AnimalDTO reported)
  {
    ingestionService.receiveAnimal(reported);
    return ResponseEntity.ok().build();
  }
}
