package com.example.desktop.api;

import com.example.desktop.dto.OutboxEntryDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public abstract class StationApi
{
  protected final RestClient rest;

  protected StationApi(String baseUrl)
  {
    this.rest = new RestClient(baseUrl);
  }

  public List<OutboxEntryDTO> outbox()
  {
    return rest.get("/outbox", new TypeReference<List<OutboxEntryDTO>>() {});
  }

  public boolean reachable()
  {
    return rest.reachable();
  }

  public String baseUrl()
  {
    return rest.baseUrl();
  }
}
