package com.example.traceability.service;

import com.example.traceability.dto.AnimalDTO;
import com.example.traceability.dto.AnimalPartDTO;
import com.example.traceability.dto.ProductDTO;
import com.example.traceability.dto.TrayDTO;
import com.example.traceability.entity.Animal;
import com.example.traceability.entity.AnimalPart;
import com.example.traceability.entity.Product;
import com.example.traceability.entity.Tray;
import com.example.traceability.repository.AnimalPartRepository;
import com.example.traceability.repository.AnimalRepository;
import com.example.traceability.repository.ProductRepository;
import com.example.traceability.repository.TrayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionService
{
  private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

  private final AnimalRepository animalRepository;
  private final AnimalPartRepository animalPartRepository;
  private final TrayRepository trayRepository;
  private final ProductRepository productRepository;

  public IngestionService(AnimalRepository animalRepository,
                          AnimalPartRepository animalPartRepository,
                          TrayRepository trayRepository,
                          ProductRepository productRepository)
  {
    this.animalRepository = animalRepository;
    this.animalPartRepository = animalPartRepository;
    this.trayRepository = trayRepository;
    this.productRepository = productRepository;
  }

  @Transactional
  public void receiveAnimal(AnimalDTO reported)
  {
    Animal animal = animalRepository.findById(reported.registrationNumber())
        .orElseGet(() -> new Animal(reported.registrationNumber(), reported.weight()));

    animal.setArrivalDateTime(reported.arrivalDateTime());
    animal.setWeight(reported.weight());
    animal.setOrigin(reported.origin());

    animalRepository.save(animal);
  }

  @Transactional
  public void receiveTray(TrayDTO reported)
  {
    Tray tray = trayRepository.findById(reported.trayId()).orElseGet(Tray::new);

    tray.setTrayId(reported.trayId());
    tray.setPartType(reported.partType());
    tray.setMaxWeight(reported.maxWeight());

    if (reported.currentWeight() != null)
    {
      tray.setCurrentWeight(reported.currentWeight());
    }

    trayRepository.save(tray);
  }

  @Transactional
  public void receivePart(AnimalPartDTO reported)
  {
    Animal animal = findOrPlaceholderAnimal(reported.animalRegistrationNumber());

    AnimalPart part = animalPartRepository.findById(reported.partId()).orElseGet(AnimalPart::new);

    part.setPartId(reported.partId());
    part.setType(reported.type());
    part.setWeight(reported.weight());
    part.setRegistrationDateTime(reported.registrationDateTime());
    part.setAnimal(animal);
    part.setTray(findTray(reported.trayId()));

    animalPartRepository.save(part);
  }

  @Transactional
  public void receiveProduct(ProductDTO reported)
  {
    List<Tray> trays = new ArrayList<>();
    for (String trayId : reported.trayIds())
    {
      trays.add(findTray(trayId));
    }

    Product product = productRepository.findById(reported.productId()).orElseGet(Product::new);

    product.setProductId(reported.productId());
    product.setProductType(reported.productType());
    product.setPackingDateTime(reported.packingDateTime());
    product.setTotalWeight(reported.totalWeight());
    product.setTrays(trays);

    productRepository.save(product);
  }

  private Animal findOrPlaceholderAnimal(int registrationNumber)
  {
    return animalRepository.findById(registrationNumber).orElseGet(() -> {
      log.info("Animal {} is not known yet, storing a placeholder until Station 1 reports it",
          registrationNumber);

      Animal placeholder = new Animal(registrationNumber, 0.0);
      placeholder.setArrivalDateTime(null);
      return animalRepository.save(placeholder);
    });
  }

  private Tray findTray(String trayId)
  {
    if (trayId == null)
    {
      return null;
    }

    return trayRepository.findById(trayId).orElseGet(() -> {
      log.info("Tray {} is not known yet, storing a placeholder until Station 2 reports it",
          trayId);

      Tray placeholder = new Tray();
      placeholder.setTrayId(trayId);
      return trayRepository.save(placeholder);
    });
  }
}
