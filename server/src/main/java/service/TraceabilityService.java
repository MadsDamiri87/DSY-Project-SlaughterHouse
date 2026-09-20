package service;

import entity.Animal;
import entity.AnimalPart;
import entity.Product;
import entity.Tray;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.AnimalPartRepository;
import repository.AnimalRepository;
import repository.ProductRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class TraceabilityService
{
  private final ProductRepository productRepository;
  private final AnimalRepository animalRepository;
  private final AnimalPartRepository animalPartRepository;

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
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new NoSuchElementException("No product with id " + productId));

    List<Tray> trays = product.getTrays();
    if (trays == null || trays.isEmpty())
    {
      return List.of();
    }

    return animalPartRepository.findByTrayIn(trays)
        .stream()
        .map(AnimalPart::getAnimal)
        .filter(Objects::nonNull)
        .map(Animal::getAnimalId)
        .distinct()
        .toList();
  }

  @Transactional(readOnly = true)
  public List<String> getProductsForAnimal(int animalId)
  {
    Animal animal = animalRepository.findById(animalId)
        .orElseThrow(() -> new NoSuchElementException("No animal with id " + animalId));

    List<Tray> trays = animalPartRepository.findByAnimal(animal)
        .stream()
        .map(AnimalPart::getTray)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (trays.isEmpty())
    {
      return List.of();
    }

    return productRepository.findDistinctByTraysIn(trays)
        .stream()
        .map(Product::getProductId)
        .distinct()
        .toList();
  }
}
