package com.example.desktop.ui;

import javafx.application.Platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Async
{
  private static final ExecutorService POOL = Executors.newCachedThreadPool(runnable ->
  {
    Thread thread = new Thread(runnable, "desktop-client-api");
    thread.setDaemon(true);
    return thread;
  });

  private Async()
  {
  }

  public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure)
  {
    POOL.submit(() ->
    {
      try
      {
        T result = work.get();
        Platform.runLater(() -> onSuccess.accept(result));
      }
      catch (Throwable failure)
      {
        Platform.runLater(() -> onFailure.accept(failure));
      }
    });
  }

  public static void shutdown()
  {
    POOL.shutdownNow();
  }
}
