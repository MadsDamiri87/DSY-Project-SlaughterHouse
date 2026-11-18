package com.example.desktop.api;

import com.example.desktop.dto.AnimalDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public class RegistrationApi extends StationApi
{
  public RegistrationApi(String baseUrl)
  {
    super(baseUrl);
  }

  public List<AnimalDTO> animals()
  {
    return rest.get("/animals", new TypeReference<List<AnimalDTO>>() {});
  }

  public AnimalDTO register(AnimalDTO animal)
  {
    return rest.post("/animals", animal, new TypeReference<AnimalDTO>() {});
  }
}
