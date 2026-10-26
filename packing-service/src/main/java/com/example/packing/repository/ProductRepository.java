package com.example.packing.repository;

import com.example.packing.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String>
{
  List<Product> findByProductType(String productType);
}
