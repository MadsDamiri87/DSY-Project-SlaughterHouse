package com.example.cuttingservice.web;

import com.example.cuttingservice.dto.AnimalPartDTO;
import com.example.cuttingservice.service.CuttingService;
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
@RequestMapping("/animal-parts")
public class AnimalPartController
{
  private final CuttingService cuttingService;

  public AnimalPartController(CuttingService cuttingService)
  {
    this.cuttingService = cuttingService;
  }

  @PostMapping
  public ResponseEntity<AnimalPartDTO> registerPart(
      @Valid @RequestBody AnimalPartDTO animalPart, UriComponentsBuilder uriBuilder)
  {
    AnimalPartDTO created = cuttingService.registerPart(animalPart);

    URI location = uriBuilder.path("/animal-parts/{partId}")
                             .buildAndExpand(created.partId())
                             .toUri();

    return ResponseEntity.created(location).body(created);
  }

  @GetMapping("/{partId}")
  public AnimalPartDTO getAnimalPart(@PathVariable String partId)
  {
    return cuttingService.findPartById(partId);
  }

  @GetMapping
  public List<AnimalPartDTO> getAnimalParts()
  {
    return cuttingService.findAllParts();
  }
}
