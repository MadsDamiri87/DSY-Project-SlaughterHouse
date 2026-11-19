package com.example.desktop.ui;

public record OutboxCount(int pending, boolean reachable)
{
  public static OutboxCount unreachable()
  {
    return new OutboxCount(0, false);
  }

  public String label()
  {
    return reachable ? String.valueOf(pending) : "—";
  }

  public String styleClass()
  {
    if (!reachable)
    {
      return "qrow-bad";
    }
    return pending > 0 ? "qrow-alarm" : "qrow-quiet";
  }
}
