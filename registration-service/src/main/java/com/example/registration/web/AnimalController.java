package com.example.registration.web;

import com.example.registration.service.AnimalRegistrationService;
import com.example.shared.dto.AnimalDTO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/animals")
public class AnimalController
{
  private final AnimalRegistrationService registrationService;

  public AnimalController(AnimalRegistrationService registrationService)
  {
    this.registrationService = registrationService;
  }

  @PostMapping
  public ResponseEntity<AnimalDTO> register(@Valid @RequestBody AnimalDTO animal,
                                            UriComponentsBuilder uriBuilder)
  {
    AnimalDTO registered = registrationService.register(animal);

    URI location = uriBuilder.path("/animals/{registrationNumber}")
        .buildAndExpand(registered.registrationNumber())
        .toUri();

    return ResponseEntity.created(location).body(registered);
  }

  @GetMapping("/{registrationNumber}")
  public AnimalDTO getAnimal(@PathVariable int registrationNumber)
  {
    return registrationService.findByRegistrationNumber(registrationNumber);
  }

  @GetMapping
  public List<AnimalDTO> getAnimals(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String origin)
  {
    if (date != null)
    {
      return registrationService.findByArrivalDate(date);
    }
    if (origin != null)
    {
      return registrationService.findByOrigin(origin);
    }
    return registrationService.findAll();
  }
}
