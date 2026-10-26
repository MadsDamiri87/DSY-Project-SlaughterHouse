package com.example.traceability.controller;

import com.example.traceability.dto.AnimalDTO;
import com.example.traceability.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
