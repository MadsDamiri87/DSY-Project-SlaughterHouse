package com.example.registration.repository;

import com.example.registration.entity.Animal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AnimalRepository extends JpaRepository<Animal, Integer>
{
  List<Animal> findByArrivalDateTimeGreaterThanEqualAndArrivalDateTimeLessThan(Instant from,
                                                                               Instant to);

  List<Animal> findByOriginIgnoreCase(String origin);
}
