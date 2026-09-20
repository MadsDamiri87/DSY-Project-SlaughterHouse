package repository;

import entity.Animal;
import entity.AnimalPart;
import entity.Tray;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnimalPartRepository extends JpaRepository<AnimalPart, String>
{
  List<AnimalPart> findByAnimal(Animal animal);

  List<AnimalPart> findByTrayIn(List<Tray> trays);
}
