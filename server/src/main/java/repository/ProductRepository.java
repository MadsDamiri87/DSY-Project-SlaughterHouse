package repository;

import entity.Product;
import entity.Tray;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String>
{

  List<Product> findDistinctByTraysIn(List<Tray> trays);
}
