package com.example.traceability.repository;

import com.example.traceability.entity.Product;
import com.example.traceability.entity.Tray;


import java.util.List;

public interface ProductRepository extends BaseRepository<Product, String>
{

  List<Product> findDistinctByTraysIn(List<Tray> trays);
}
