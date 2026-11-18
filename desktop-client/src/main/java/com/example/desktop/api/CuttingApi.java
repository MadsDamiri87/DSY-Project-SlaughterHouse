package com.example.desktop.api;

import com.example.desktop.dto.AnimalPartDTO;
import com.example.desktop.dto.TrayDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public class CuttingApi extends StationApi
{
  public CuttingApi(String baseUrl)
  {
    super(baseUrl);
  }

  public List<AnimalPartDTO> parts()
  {
    return rest.get("/animal-parts", new TypeReference<List<AnimalPartDTO>>() {});
  }

  public List<TrayDTO> trays()
  {
    return rest.get("/trays", new TypeReference<List<TrayDTO>>() {});
  }

  public AnimalPartDTO registerPart(AnimalPartDTO part)
  {
    return rest.post("/animal-parts", part, new TypeReference<AnimalPartDTO>() {});
  }

  public TrayDTO createTray(TrayDTO tray)
  {
    return rest.post("/trays", tray, new TypeReference<TrayDTO>() {});
  }

  public AnimalPartDTO placeInTray(String partId, String trayId)
  {
    return rest.put("/trays/" + trayId + "/animal-parts/" + partId,
                    new TypeReference<AnimalPartDTO>() {});
  }
}
