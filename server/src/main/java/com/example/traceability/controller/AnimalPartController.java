package com.example.traceability.controller;

import com.example.traceability.dto.AnimalPartDTO;
import com.example.traceability.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Kaldes ikke længere af cutting-service. Stationens outbox sender nu
// PART_REGISTERED og PART_PLACED_IN_TRAY over AMQP med routingnøglerne
// part.registered og part.placed, som serverens lytter på køen
// traceability.part modtager.
// Endepunktet er bevaret, så modtagelsen også kan afprøves med Postman.

@RestController
@RequestMapping("/animal-parts")
public class AnimalPartController
{
  private final IngestionService ingestionService;

  public AnimalPartController(IngestionService ingestionService)
  {
    this.ingestionService = ingestionService;
  }

  @PostMapping
  public ResponseEntity<Void> receive(@Valid @RequestBody AnimalPartDTO reported)
  {
    ingestionService.receivePart(reported);
    return ResponseEntity.ok().build();
  }
}
