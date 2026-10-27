package com.example.cuttingservice.repository;

import com.example.cuttingservice.entity.Tray;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrayRepository extends JpaRepository<Tray, String>
{
  List<Tray> findByPartType(String type);
}
