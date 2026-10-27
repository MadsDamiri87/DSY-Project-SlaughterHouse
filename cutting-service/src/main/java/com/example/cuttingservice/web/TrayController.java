package com.example.cuttingservice.web;

import com.example.cuttingservice.dto.AnimalPartDTO;
import com.example.cuttingservice.dto.PartPlacementDTO;
import com.example.cuttingservice.dto.TrayDTO;
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
@RequestMapping("/trays")
public class TrayController
{
  private final CuttingService cuttingService;

  public TrayController(CuttingService cuttingService)
  {
    this.cuttingService = cuttingService;
  }

  @PostMapping
  public ResponseEntity<TrayDTO> createTray(@Valid @RequestBody TrayDTO tray,
                                            UriComponentsBuilder uriBuilder)
  {
    TrayDTO created = cuttingService.createTray(tray);

    URI location = uriBuilder.path("/trays/{trayId}")
        .buildAndExpand(created.trayId())
        .toUri();

    return ResponseEntity.created(location).body(created);
  }

  @GetMapping("/{trayId}")
  public TrayDTO getTray(@PathVariable String trayId)
  {
    return cuttingService.findTrayById(trayId);
  }

  @GetMapping
  public List<TrayDTO> getTrays()
  {
    return cuttingService.findAllTrays();
  }

  @PostMapping("/{trayId}/animal-parts")
  public AnimalPartDTO placePartInTray(@PathVariable String trayId,
                                       @Valid @RequestBody PartPlacementDTO placement)
  {
    return cuttingService.placeInTray(placement.partId(), trayId);
  }
}
