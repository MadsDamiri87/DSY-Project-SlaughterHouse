package com.example.desktop.ui;

import com.example.desktop.api.Apis;
import com.example.desktop.domain.Station;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Health
{
  private static final int PERIOD_SECONDS = 5;

  private final Apis apis;
  private final BooleanProperty backendUp = new SimpleBooleanProperty(true);
  private final Map<Station, ObjectProperty<OutboxCount>> counts = new EnumMap<>(Station.class);
  private final ScheduledExecutorService scheduler;

  public Health(Apis apis)
  {
    this.apis = apis;

    for (Station station : Station.all())
    {
      counts.put(station, new SimpleObjectProperty<>(OutboxCount.unreachable()));
    }

    this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable ->
    {
      Thread thread = new Thread(runnable, "desktop-client-health");
      thread.setDaemon(true);
      return thread;
    });
  }

  public BooleanProperty backendUpProperty()
  {
    return backendUp;
  }

  public ObjectProperty<OutboxCount> countProperty(Station station)
  {
    return counts.get(station);
  }

  public void start()
  {
    scheduler.scheduleWithFixedDelay(this::poll, 0, PERIOD_SECONDS, TimeUnit.SECONDS);
  }

  public void stop()
  {
    scheduler.shutdownNow();
  }

  private void poll()
  {
    boolean up = apis.backendReachable();
    Map<Station, OutboxCount> polled = new EnumMap<>(Station.class);

    for (Station station : Station.all())
    {
      polled.put(station, countFor(station));
    }

    Platform.runLater(() ->
    {
      backendUp.set(up);
      polled.forEach((station, count) -> counts.get(station).set(count));
    });
  }

  private OutboxCount countFor(Station station)
  {
    try
    {
      return new OutboxCount(apis.station(station).outbox().size(), true);
    }
    catch (RuntimeException exception)
    {
      return OutboxCount.unreachable();
    }
  }
}
