package com.example.registration.service;

import com.example.registration.entity.Animal;
import com.example.registration.repository.AnimalRepository;
import com.example.registration.dto.AnimalDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnimalRegistrationService
{
  private final AnimalRepository animalRepository;

  public AnimalRegistrationService(AnimalRepository animalRepository)
  {
    this.animalRepository = animalRepository;
  }

  @Transactional
  public AnimalDTO register(AnimalDTO animal)
  {
    if (animalRepository.existsById(animal.registrationNumber()))
    {
      throw new DuplicateRegistrationException(animal.registrationNumber());
    }

    LocalDateTime arrival =
        animal.arrivalDateTime() != null ? animal.arrivalDateTime() : LocalDateTime.now();

    Animal saved = animalRepository.save(
        new Animal(animal.registrationNumber(), arrival, animal.weight(), animal.origin()));

    return toDto(saved);
  }

  @Transactional(readOnly = true)
  public AnimalDTO findByRegistrationNumber(int registrationNumber)
  {
    return animalRepository.findById(registrationNumber)
        .map(this::toDto)
        .orElseThrow(() -> new AnimalNotFoundException(registrationNumber));
  }

  @Transactional(readOnly = true)
  public List<AnimalDTO> findByArrivalDate(LocalDate arrivalDate)
  {
    return animalRepository
        .findByArrivalDateTimeGreaterThanEqualAndArrivalDateTimeLessThan(arrivalDate.atStartOfDay(),
            arrivalDate.plusDays(1).atStartOfDay())
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AnimalDTO> findByOrigin(String origin)
  {
    return animalRepository.findByOriginIgnoreCase(origin)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AnimalDTO> findAll()
  {
    return animalRepository.findAll().stream().map(this::toDto).toList();
  }

  private AnimalDTO toDto(Animal animal)
  {
    return new AnimalDTO(animal.getRegistrationNumber(), animal.getArrivalDateTime(),
        animal.getWeight(), animal.getOrigin());
  }
}
