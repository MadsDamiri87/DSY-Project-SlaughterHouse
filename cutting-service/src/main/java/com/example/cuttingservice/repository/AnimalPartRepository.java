package com.example.cuttingservice.repository;

import com.example.cuttingservice.entity.AnimalPart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalPartRepository extends JpaRepository<AnimalPart, String>
{
}
