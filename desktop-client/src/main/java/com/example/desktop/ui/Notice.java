package com.example.desktop.ui;

import com.example.desktop.api.ApiException;
import com.example.desktop.api.BackendUnavailableException;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class Notice extends VBox
{
  private final VBox error = new VBox(3);
  private final Label errorTitle = Ui.label("", "notice-error-title");
  private final Label errorBody = Ui.label("", "notice-error-body");

  private final HBox receipt = new HBox(8);
  private final Label receiptTitle = Ui.label("", "notice-ok-title");
  private final Label receiptBody = Ui.label("", "notice-ok-body", "mono");

  public Notice()
  {
    error.getStyleClass().add("notice-error");
    error.getChildren().addAll(errorTitle, errorBody);
    error.setMaxWidth(560);
    errorBody.setWrapText(true);

    Circle dot = Ui.dot(4);
    dot.getStyleClass().add("dot-up");
    receipt.setAlignment(Pos.CENTER_LEFT);
    receipt.getChildren().addAll(dot, receiptTitle, receiptBody);

    getChildren().addAll(error, receipt);
    clear();
  }

  public void showError(Throwable failure)
  {
    if (failure instanceof BackendUnavailableException)
    {
      showError("Ikke forbundet", failure.getMessage());
      return;
    }

    if (failure instanceof ApiException apiException)
    {
      showError(apiException.status() + " — Afvist", apiException.getMessage());
      return;
    }

    showError("Fejl", String.valueOf(failure.getMessage()));
  }

  public void showError(String title, String body)
  {
    errorTitle.setText(title.toUpperCase());
    errorBody.setText(body);
    show(error);
    hide(receipt);
  }

  public void showReceipt(String title, String body)
  {
    receiptTitle.setText(title.toUpperCase());
    receiptBody.setText(body);
    show(receipt);
    hide(error);
  }

  public void clear()
  {
    hide(error);
    hide(receipt);
  }

  private void show(javafx.scene.Node node)
  {
    node.setVisible(true);
    node.setManaged(true);
  }

  private void hide(javafx.scene.Node node)
  {
    node.setVisible(false);
    node.setManaged(false);
  }
}
