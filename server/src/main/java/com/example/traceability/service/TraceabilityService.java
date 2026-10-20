package com.example.traceability.service;

import com.example.traceability.entity.Animal;
import com.example.traceability.entity.AnimalPart;
import com.example.traceability.entity.Product;
import com.example.traceability.entity.Tray;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.traceability.repository.AnimalPartRepository;
import com.example.traceability.repository.AnimalRepository;
import com.example.traceability.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TraceabilityService
{
  private final ProductRepository productRepository;
  private final AnimalRepository animalRepository;
  private final AnimalPartRepository animalPartRepository;

  private final AtomicInteger casesRegistered = new AtomicInteger();
  private volatile TraceabilityCase currentCase;

  public TraceabilityService(ProductRepository productRepository, AnimalRepository animalRepository,
                             AnimalPartRepository animalPartRepository)
  {
    this.productRepository = productRepository;
    this.animalRepository = animalRepository;
    this.animalPartRepository = animalPartRepository;
  }

  @Transactional(readOnly = true)
  public List<Integer> getAnimalsForProduct(String productId)
  {
    String subject = "product " + productId;

    Product product = productRepository.findById(productId).orElse(null);
    if (product == null)
    {
      registerCase(subject, CaseStatus.NOT_FOUND);
      throw new NoSuchElementException("No product with id " + productId);
    }

    List<Tray> trays = product.getTrays();
    if (trays == null || trays.isEmpty())
    {
      registerCase(subject, CaseStatus.COMPLETED);
      return List.of();
    }

    List<Integer> registrationNumbers = animalPartRepository.findByTrayIn(trays)
        .stream()
        .map(AnimalPart::getAnimal)
        .filter(Objects::nonNull)
        .map(Animal::getRegistrationNumber)
        .distinct()
        .toList();

    registerCase(subject, CaseStatus.COMPLETED);
    return registrationNumbers;
  }

  @Transactional(readOnly = true)
  public List<String> getProductsForAnimal(int registrationNumber)
  {
    String subject = "animal " + registrationNumber;

    Animal animal = animalRepository.findById(registrationNumber).orElse(null);
    if (animal == null)
    {
      registerCase(subject, CaseStatus.NOT_FOUND);
      throw new NoSuchElementException("No animal with registration number " + registrationNumber);
    }

    List<Tray> trays = animalPartRepository.findByAnimal(animal)
        .stream()
        .map(AnimalPart::getTray)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (trays.isEmpty())
    {
      registerCase(subject, CaseStatus.COMPLETED);
      return List.of();
    }

    List<String> productIds = productRepository.findDistinctByTraysIn(trays)
        .stream()
        .map(Product::getProductId)
        .distinct()
        .toList();

    registerCase(subject, CaseStatus.COMPLETED);
    return productIds;
  }

  public int getCasesRegistered()
  {
    return casesRegistered.get();
  }

  public Optional<TraceabilityCase> getCurrentCase()
  {
    return Optional.ofNullable(currentCase);
  }

  private void registerCase(String subject, CaseStatus status)
  {
    casesRegistered.incrementAndGet();
    currentCase = new TraceabilityCase(subject, LocalDateTime.now(), status);
  }
}
