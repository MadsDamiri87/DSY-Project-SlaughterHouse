package com.example.traceability.controller;

import com.example.traceability.dto.AnimalPartDTO;
import com.example.traceability.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
