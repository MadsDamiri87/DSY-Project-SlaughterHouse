package com.example.registration.repository;

import com.example.registration.entity.AnimalRegistrationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnimalRegistrationOutboxRepository
    extends JpaRepository<AnimalRegistrationOutbox, UUID>
{
  List<AnimalRegistrationOutbox> findTop100ByOrderByOccurredAtAsc();
}
