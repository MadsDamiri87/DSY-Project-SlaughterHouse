package com.example.traceability.controller;

import com.example.traceability.dto.TrayDTO;
import com.example.traceability.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trays")
public class TrayController
{
  private final IngestionService ingestionService;

  public TrayController(IngestionService ingestionService)
  {
    this.ingestionService = ingestionService;
  }

  @PostMapping
  public ResponseEntity<Void> receive(@Valid @RequestBody TrayDTO reported)
  {
    ingestionService.receiveTray(reported);
    return ResponseEntity.ok().build();
  }
}
