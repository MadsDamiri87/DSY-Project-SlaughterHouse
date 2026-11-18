package com.example.desktop.ui;

import javafx.scene.Node;

public interface View
{
  String title();

  Node node();

  default void onShown()
  {
  }
}
