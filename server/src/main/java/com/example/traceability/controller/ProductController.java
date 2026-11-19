package com.example.traceability.controller;

import com.example.traceability.dto.ProductDTO;
import com.example.traceability.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Kaldes ikke længere af packing-service. Stationens outbox sender nu
// PRODUCT_PACKED over AMQP med routingnøglen product.packed, som serverens
// lytter på køen traceability.product modtager.
// Endepunktet er bevaret, så modtagelsen også kan afprøves med Postman.

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
