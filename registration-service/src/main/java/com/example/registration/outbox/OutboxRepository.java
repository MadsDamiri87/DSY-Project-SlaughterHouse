package com.example.registration.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEntry, Long>
{
  List<OutboxEntry> findTop100ByStatusOrderByOccurredAtAsc(OutboxStatus status);
}
