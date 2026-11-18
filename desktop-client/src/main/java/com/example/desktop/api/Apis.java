package com.example.desktop.api;

import com.example.desktop.domain.Config;
import com.example.desktop.domain.Station;

public class Apis implements AutoCloseable
{
  private final Config config;
  private final RegistrationApi registration;
  private final CuttingApi cutting;
  private final PackingApi packing;
  private final TraceApi trace;
  private final RestClient backend;

  public Apis(Config config)
  {
    this.config = config;
    this.registration = new RegistrationApi(config.registrationUrl());
    this.cutting = new CuttingApi(config.cuttingUrl());
    this.packing = new PackingApi(config.packingUrl());
    this.trace = new TraceApi(config.traceHost(), config.tracePort());
    this.backend = new RestClient(config.backendUrl());
  }

  public Config config()
  {
    return config;
  }

  public RegistrationApi registration()
  {
    return registration;
  }

  public CuttingApi cutting()
  {
    return cutting;
  }

  public PackingApi packing()
  {
    return packing;
  }

  public TraceApi trace()
  {
    return trace;
  }

  public StationApi station(Station station)
  {
    return switch (station)
    {
      case REGISTRATION -> registration;
      case CUTTING -> cutting;
      case PACKING -> packing;
    };
  }

  public boolean backendReachable()
  {
    return backend.reachable();
  }

  @Override
  public void close()
  {
    trace.close();
  }
}
