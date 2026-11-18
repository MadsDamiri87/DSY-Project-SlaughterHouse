package com.example.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class AppDialog
{
  private final Stage stage = new Stage();
  private boolean confirmed;

  public AppDialog(Window owner, String title, Node content, String confirmText)
  {
    Button confirm = Ui.primary(confirmText);
    confirm.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    confirm.setOnAction(event -> close(true));

    Button cancel = Ui.ghost("Annullér");
    cancel.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    cancel.setOnAction(event -> close(false));

    HBox buttons = Ui.row(12, Ui.spacer(), cancel, confirm);
    buttons.setAlignment(Pos.CENTER_RIGHT);

    VBox body = Ui.column(22, content, buttons);
    body.setPadding(new Insets(24));

    BorderPane root = new BorderPane();
    root.getStyleClass().add("window");
    root.setTop(buildTopBar(title));
    root.setCenter(body);

    Scene scene = new Scene(root);
    scene.setFill(Color.TRANSPARENT);

    if (owner != null && owner.getScene() != null)
    {
      scene.getStylesheets().addAll(owner.getScene().getStylesheets());
    }

    scene.setOnKeyPressed(event ->
    {
      if (event.getCode() == KeyCode.ESCAPE)
      {
        close(false);
      }

      if (event.getCode() == KeyCode.ENTER)
      {
        close(true);
      }
    });

    stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.initStyle(StageStyle.TRANSPARENT);
    stage.setScene(scene);
    stage.setResizable(false);
  }

  public boolean showAndWait()
  {
    stage.showAndWait();
    return confirmed;
  }

  private HBox buildTopBar(String title)
  {
    HBox closeButton = WindowChrome.closeOnly(stage);

    HBox bar = Ui.row(0, Ui.label(title.toUpperCase(), "brand"), Ui.spacer(), closeButton);
    bar.getStyleClass().add("top-bar");
    bar.setAlignment(Pos.CENTER_LEFT);
    HBox.setMargin(closeButton, new Insets(0, -20, 0, 14));

    WindowChrome.makeDraggable(bar, stage);

    return bar;
  }

  private void close(boolean accepted)
  {
    confirmed = accepted;
    stage.close();
  }
}
