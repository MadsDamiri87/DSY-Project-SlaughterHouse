package com.example.packing.service;

import com.example.packing.entity.Product;
import com.example.packing.outbox.OutboxEntry;
import com.example.packing.outbox.OutboxEventType;
import com.example.packing.outbox.OutboxRepository;
import com.example.packing.repository.ProductRepository;
import com.example.packing.dto.ProductDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Service
public class PackingService
{
  private final OutboxRepository outboxRepository;
  private final ProductRepository productRepository;
  private final ObjectMapper objectMapper;


  public PackingService(OutboxRepository outboxRepository, ProductRepository productRepository,
                        ObjectMapper objectMapper)
  {

    this.outboxRepository  = outboxRepository;
    this.productRepository = productRepository;
    this.objectMapper      = objectMapper;
  }

  @Transactional
  public ProductDTO pack(ProductDTO product)
  {
    if (productRepository.existsById(product.productId()))
    {
      throw new DuplicateProductException(product.productId());
    }

    Instant packedAt =
        product.packingDateTime() != null ? product.packingDateTime() : Instant.now();

    Product saved = productRepository.save(
        new Product(product.productId(), product.productType(), packedAt, product.totalWeight(),
                    List.copyOf(product.trayIds())));

    ProductDTO packed = toDto(saved);
    outboxRepository.save(OutboxEntry.of(OutboxEventType.PRODUCT_PACKED, toJson(packed)));

    return packed;
  }

  @Transactional(readOnly = true)
  public ProductDTO findById(String productId)
  {
    return productRepository.findById(productId).map(this::toDto)
                            .orElseThrow(() -> new ProductNotFoundException(productId));
  }



  @Transactional(readOnly = true)
  public List<ProductDTO> findAll()
  {
    return productRepository.findAll().stream().map(this::toDto).toList();
  }

  private String toJson(ProductDTO product)
  {
    try
    {
      return objectMapper.writeValueAsString(product);
    }
    catch (JsonProcessingException exception)
    {
      throw new IllegalStateException("Could not serialise product: " + product.productId(), exception);
    }
  }

  private ProductDTO toDto(Product product)
  {
    return new ProductDTO(product.getProductId(), product.getProductType(), product.getPackingDateTime(),
                          product.getTotalWeight(), product.getTrayIds());
  }
}
