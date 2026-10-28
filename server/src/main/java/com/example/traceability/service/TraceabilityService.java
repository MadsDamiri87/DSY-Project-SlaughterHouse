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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  @Transactional(readOnly = true)
  public AnimalTrace traceAnimal(int registrationNumber)
  {
    String subject = "animal " + registrationNumber;

    Animal animal = animalRepository.findById(registrationNumber).orElse(null);
    if (animal == null)
    {
      registerCase(subject, CaseStatus.NOT_FOUND);
      throw new NoSuchElementException("No animal with registration number " + registrationNumber);
    }

    List<AnimalPart> parts = animalPartRepository.findByAnimal(animal);
    Map<String, List<String>> productsByTray = findProductsByTray(parts);

    List<TracedPart> tracedParts = parts.stream()
        .map(part -> toTracedPart(part, productsByTray))
        .toList();

    registerCase(subject, CaseStatus.COMPLETED);

    return new AnimalTrace(animal.getRegistrationNumber(), animal.getSpecies(), animal.getOrigin(),
        animal.getWeight(), animal.getArrivalDateTime(), tracedParts);
  }

  @Transactional(readOnly = true)
  public PartTrace tracePart(String partId)
  {
    String subject = "part " + partId;

    AnimalPart part = animalPartRepository.findById(partId).orElse(null);
    if (part == null)
    {
      registerCase(subject, CaseStatus.NOT_FOUND);
      throw new NoSuchElementException("No animal part with id " + partId);
    }

    TracedPart tracedPart = toTracedPart(part, findProductsByTray(List.of(part)));
    Animal animal = part.getAnimal();

    registerCase(subject, CaseStatus.COMPLETED);

    return new PartTrace(tracedPart,
        animal == null ? 0 : animal.getRegistrationNumber(),
        animal == null ? null : animal.getSpecies(),
        animal == null ? null : animal.getOrigin());
  }

  public int getCasesRegistered()
  {
    return casesRegistered.get();
  }

  public Optional<TraceabilityCase> getCurrentCase()
  {
    return Optional.ofNullable(currentCase);
  }

  private Map<String, List<String>> findProductsByTray(List<AnimalPart> parts)
  {
    List<Tray> trays = parts.stream()
        .map(AnimalPart::getTray)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (trays.isEmpty())
    {
      return Map.of();
    }

    Map<String, List<String>> productsByTray = new HashMap<>();
    for (Product product : productRepository.findDistinctByTraysIn(trays))
    {
      for (Tray tray : product.getTrays())
      {
        productsByTray.computeIfAbsent(tray.getTrayId(), id -> new ArrayList<>())
            .add(product.getProductId());
      }
    }

    return productsByTray;
  }

  private TracedPart toTracedPart(AnimalPart part, Map<String, List<String>> productsByTray)
  {
    Tray tray = part.getTray();
    String trayId = tray == null ? null : tray.getTrayId();

    return new TracedPart(part.getPartId(), part.getType(), part.getWeight(),
        part.getRegistrationDateTime(), trayId,
        trayId == null ? List.of() : productsByTray.getOrDefault(trayId, List.of()));
  }

  private void registerCase(String subject, CaseStatus status)
  {
    casesRegistered.incrementAndGet();
    currentCase = new TraceabilityCase(subject, Instant.now(), status);
  }
}
