package com.example.packing.controller;

import com.example.packing.dto.ProductDTO;
import com.example.packing.service.PackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController
{
  private final PackingService packingService;

  public ProductController(PackingService packingService)
  {
    this.packingService = packingService;
  }

  @PostMapping
  public ResponseEntity<ProductDTO> packProduct(@Valid @RequestBody ProductDTO product,
                                                UriComponentsBuilder uriBuilder)
  {
    ProductDTO packed = packingService.pack(product);

    URI location = uriBuilder.path("/products/{productId}")
        .buildAndExpand(packed.productId())
        .toUri();

    return ResponseEntity.created(location).body(packed);
  }

  @GetMapping("/{productId}")
  public ProductDTO getProduct(@PathVariable String productId)
  {
    return packingService.findById(productId);
  }

  @GetMapping
  public List<ProductDTO> getProducts()
  {
    return packingService.findAll();
  }
}
