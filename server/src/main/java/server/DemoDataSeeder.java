package server;

import entity.Animal;
import entity.AnimalPart;
import entity.Product;
import entity.Tray;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import repository.AnimalPartRepository;
import repository.AnimalRepository;
import repository.ProductRepository;
import repository.TrayRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner
{
  private final AnimalRepository animalRepository;
  private final TrayRepository trayRepository;
  private final AnimalPartRepository animalPartRepository;
  private final ProductRepository productRepository;

  public DemoDataSeeder(AnimalRepository animalRepository, TrayRepository trayRepository,
                        AnimalPartRepository animalPartRepository, ProductRepository productRepository)
  {
    this.animalRepository = animalRepository;
    this.trayRepository = trayRepository;
    this.animalPartRepository = animalPartRepository;
    this.productRepository = productRepository;
  }

  @Override
  @Transactional
  public void run(String... args)
  {
    if (animalRepository.count() > 0)
    {
      System.out.println("Demo data already present, skipping seed");
      return;
    }

    Animal pig1 = animal(1, "Pig", 95.0);
    Animal pig2 = animal(2, "Pig", 102.5);
    Animal pig3 = animal(3, "Pig", 88.0);
    animalRepository.saveAll(List.of(pig1, pig2, pig3));

    Tray loinTray = tray("TRAY-LOIN-1", "LOIN", 50.0);
    Tray bellyTray = tray("TRAY-BELLY-1", "BELLY", 40.0);
    Tray ribTray = tray("TRAY-RIB-1", "RIB", 30.0);
    trayRepository.saveAll(List.of(loinTray, bellyTray, ribTray));

    animalPartRepository.saveAll(List.of(
        part("PART-1", "LOIN", 6.2, pig1, loinTray),
        part("PART-2", "LOIN", 6.8, pig2, loinTray),
        part("PART-3", "BELLY", 4.5, pig1, bellyTray),
        part("PART-4", "RIB", 3.1, pig3, ribTray)));

    Product loinPack = product("PROD-LOIN-PACK", "LOIN_PACK", 13.0, List.of(loinTray));
    Product halfAnimal = product("PROD-HALF-ANIMAL", "HALF_ANIMAL", 17.5, List.of(loinTray, bellyTray));
    productRepository.saveAll(List.of(loinPack, halfAnimal));

    System.out.println("Demo data seeded");
  }

  private Animal animal(int id, String species, double weight)
  {
    Animal animal = new Animal(id, weight);
    animal.setSpecies(species);
    animal.setSlaughterDateTime(LocalDateTime.now());
    return animal;
  }

  private Tray tray(String id, String partType, double maxWeight)
  {
    Tray tray = new Tray();
    tray.setTrayId(id);
    tray.setPartType(partType);
    tray.setMaxWeight(maxWeight);
    return tray;
  }

  private AnimalPart part(String id, String type, double weight, Animal animal, Tray tray)
  {
    AnimalPart part = new AnimalPart();
    part.setPartId(id);
    part.setType(type);
    part.setWeight(weight);
    part.setAnimal(animal);
    part.setTray(tray);
    return part;
  }

  private Product product(String id, String type, double totalWeight, List<Tray> trays)
  {
    Product product = new Product();
    product.setProductId(id);
    product.setProductType(type);
    product.setTotalWeight(totalWeight);
    product.setPackingDateTime(LocalDateTime.now());
    product.setTrays(new java.util.ArrayList<>(trays));
    return product;
  }
}
