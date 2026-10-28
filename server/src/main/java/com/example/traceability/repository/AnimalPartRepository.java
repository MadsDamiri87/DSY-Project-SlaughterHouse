package com.example.traceability.repository;

import com.example.traceability.entity.Animal;
import com.example.traceability.entity.AnimalPart;
import com.example.traceability.entity.Tray;

import java.util.List;

public interface AnimalPartRepository extends BaseRepository<AnimalPart, String>
{
  List<AnimalPart> findByAnimal(Animal animal);

  List<AnimalPart> findByTrayIn(List<Tray> trays);
}
