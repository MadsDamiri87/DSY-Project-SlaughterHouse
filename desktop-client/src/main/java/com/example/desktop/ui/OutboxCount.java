package com.example.desktop.ui;

public record OutboxCount(int pending, int failed, boolean reachable)
{
  public static OutboxCount unreachable()
  {
    return new OutboxCount(0, 0, false);
  }

  public String label()
  {
    if (!reachable)
    {
      return "—";
    }
    if (failed > 0)
    {
      return failed + " fejlet";
    }
    return String.valueOf(pending);
  }

  public String styleClass()
  {
    if (failed > 0)
    {
      return "qrow-bad";
    }
    if (pending > 0)
    {
      return "qrow-alarm";
    }
    return "qrow-quiet";
  }
}
