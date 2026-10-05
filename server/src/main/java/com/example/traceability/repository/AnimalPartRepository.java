package com.example.traceability.repository;

import com.example.traceability.entity.Animal;
import com.example.traceability.entity.AnimalPart;
import com.example.traceability.entity.Tray;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnimalPartRepository extends JpaRepository<AnimalPart, String>
{
  List<AnimalPart> findByAnimal(Animal animal);

  List<AnimalPart> findByTrayIn(List<Tray> trays);
}
