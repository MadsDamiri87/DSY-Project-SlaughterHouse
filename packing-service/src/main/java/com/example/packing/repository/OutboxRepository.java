package com.example.packing.repository;

import com.example.packing.outbox.OutboxEntry;
import com.example.packing.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEntry, Long>
{
  List<OutboxEntry> findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus status);
}
