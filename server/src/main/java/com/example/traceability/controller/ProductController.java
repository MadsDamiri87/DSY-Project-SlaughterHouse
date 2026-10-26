package com.example.traceability.controller;

import com.example.traceability.dto.ProductDTO;
import com.example.traceability.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController
{
  private final IngestionService ingestionService;

  public ProductController(IngestionService ingestionService)
  {
    this.ingestionService = ingestionService;
  }

  @PostMapping
  public ResponseEntity<Void> receive(@Valid @RequestBody ProductDTO reported)
  {
    ingestionService.receiveProduct(reported);
    return ResponseEntity.ok().build();
  }
}
