package com.example.traceability.repository;

import com.example.traceability.entity.Animal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalRepository extends JpaRepository<Animal, Integer>
{


}
