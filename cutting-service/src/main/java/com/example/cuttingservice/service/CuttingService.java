package com.example.cuttingservice.service;

import com.example.cuttingservice.dto.AnimalPartDTO;
import com.example.cuttingservice.dto.TrayDTO;
import com.example.cuttingservice.entity.AnimalPart;
import com.example.cuttingservice.entity.Tray;
import com.example.cuttingservice.outbox.OutboxEntry;
import com.example.cuttingservice.outbox.OutboxEventType;
import com.example.cuttingservice.outbox.OutboxRepository;
import com.example.cuttingservice.repository.AnimalPartRepository;
import com.example.cuttingservice.repository.TrayRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CuttingService
{
  private final AnimalPartRepository animalPartRepository;
  private final TrayRepository trayRepository;
  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;

  public CuttingService(AnimalPartRepository animalPartRepository, TrayRepository trayRepository,
                        OutboxRepository outboxRepository, ObjectMapper objectMapper)
  {
    this.animalPartRepository = animalPartRepository;
    this.trayRepository       = trayRepository;
    this.outboxRepository     = outboxRepository;
    this.objectMapper         = objectMapper;
  }

  @Transactional
  public AnimalPartDTO registerPart(AnimalPartDTO reported)
  {
    if (animalPartRepository.existsById(reported.partId()))
    {
      throw new DuplicateAnimalPartException(reported.partId());
    }

    Instant registeredAt =
        reported.registrationDateTime() != null ? reported.registrationDateTime() : Instant.now();

    AnimalPart saved = animalPartRepository.save(
        new AnimalPart(reported.partId(), reported.type(), reported.weight(),
                       reported.animalRegistrationNumber(), registeredAt));

    AnimalPartDTO registered = toDto(saved);
    outboxRepository.save(OutboxEntry.of(OutboxEventType.PART_REGISTERED, toJson(registered)));

    return registered;
  }

  @Transactional
  public TrayDTO createTray(TrayDTO reported)
  {
    if (trayRepository.existsById(reported.trayId()))
    {
      throw new DuplicateTrayException(reported.trayId());
    }

    Tray saved = trayRepository.save(
        new Tray(reported.trayId(), reported.partType(), reported.maxWeight()));

    TrayDTO created = toDto(saved);
    outboxRepository.save(OutboxEntry.of(OutboxEventType.TRAY_CREATED, toJson(created)));

    return created;
  }

  @Transactional
  public AnimalPartDTO placeInTray(String partId, String trayId)
  {
    AnimalPart part = animalPartRepository.findById(partId)
                                          .orElseThrow(() -> new AnimalPartNotFoundException(partId));

    Tray tray = trayRepository.findById(trayId)
                              .orElseThrow(() -> new TrayNotFoundException(trayId));

    tray.addPart(part);

    AnimalPartDTO placed = toDto(part);
    outboxRepository.save(OutboxEntry.of(OutboxEventType.PART_PLACED_IN_TRAY, toJson(placed)));

    return placed;
  }

  @Transactional(readOnly = true)
  public AnimalPartDTO findPartById(String partId)
  {
    return animalPartRepository.findById(partId).map(this::toDto)
                               .orElseThrow(() -> new AnimalPartNotFoundException(partId));
  }

  @Transactional(readOnly = true)
  public TrayDTO findTrayById(String trayId)
  {
    return trayRepository.findById(trayId).map(this::toDto)
                         .orElseThrow(() -> new TrayNotFoundException(trayId));
  }

  @Transactional(readOnly = true)
  public List<AnimalPartDTO> findAllParts()
  {
    return animalPartRepository.findAll().stream().map(this::toDto).toList();
  }

  private String toJson(Object payload)
  {
    try
    {
      return objectMapper.writeValueAsString(payload);
    }
    catch (JsonProcessingException exception)
    {
      throw new IllegalStateException("Could not serialise: " + payload, exception);
    }
  }

  private AnimalPartDTO toDto(AnimalPart animalPart)
  {
    Tray tray = animalPart.getTray();

    return new AnimalPartDTO(animalPart.getPartId(), animalPart.getType(), animalPart.getWeight(),
                             animalPart.getAnimalRegistrationNumber(),
                             animalPart.getRegistrationDateTime(),
                             tray != null ? tray.getTrayId() : null);
  }

  private TrayDTO toDto(Tray tray)
  {
    return new TrayDTO(tray.getTrayId(), tray.getPartType(), tray.getMaxWeight(),
                       tray.getCurrentWeight());
  }
}
