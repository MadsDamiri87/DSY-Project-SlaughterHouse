package com.example.registration.web;

import com.example.registration.dto.OutboxEntryDTO;
import com.example.registration.outbox.OutboxEntry;
import com.example.registration.outbox.OutboxRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/outbox")
public class OutboxController
{
  private final OutboxRepository outboxRepository;

  public OutboxController(OutboxRepository outboxRepository)
  {
    this.outboxRepository = outboxRepository;
  }

  @GetMapping
  public List<OutboxEntryDTO> getEntries()
  {
    return outboxRepository.findAll(Sort.by(Sort.Direction.DESC, "occurredAt"))
        .stream()
        .map(this::toDTO)
        .toList();
  }

  private OutboxEntryDTO toDTO(OutboxEntry entry)
  {
    return new OutboxEntryDTO(entry.getId(),
                              entry.getEventId().toString(),
                              entry.getType().name(),
                              entry.getPayload(),
                              entry.getOccurredAt(),
                              entry.getStatus().name());
  }
}
