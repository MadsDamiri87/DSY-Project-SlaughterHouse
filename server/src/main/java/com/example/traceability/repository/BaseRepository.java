package com.example.traceability.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends Repository<T, ID>
{
  <S extends T> S save(S entity);

  Optional<T> findById(ID id);

  List<T> findAll();
}
