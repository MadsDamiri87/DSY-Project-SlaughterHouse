package com.example.registration.repository;

import com.example.registration.entity.Animal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnimalRepository extends JpaRepository<Animal, Integer>
{
  List<Animal> findByArrivalDateTimeGreaterThanEqualAndArrivalDateTimeLessThan(LocalDateTime from,
                                                                               LocalDateTime to);

  List<Animal> findByOriginIgnoreCase(String origin);
}
