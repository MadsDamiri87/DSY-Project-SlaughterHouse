package com.example.desktop.ui;

import com.example.desktop.auth.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class LoginView extends BorderPane
{
  private static final double FORM_WIDTH = 420;

  private final TextField username = Ui.field("");
  private final PasswordField password = new PasswordField();
  private final Label error = Ui.label("", "login-error");

  public LoginView(Stage stage, Consumer<Session> onLogin)
  {
    WindowChrome.markAsWindow(this, stage);
    setTop(buildTopBar(stage));
    setCenter(buildForm(onLogin));
  }

  public void focusUsername()
  {
    username.requestFocus();
  }

  private HBox buildTopBar(Stage stage)
  {
    Circle dot = Ui.dot(4);
    dot.getStyleClass().add("dot-down");

    Label live = Ui.label("IKKE FORBUNDET", "live", "live-down");

    HBox windowButtons = WindowChrome.buttons(stage);

    HBox right = Ui.row(18, Ui.row(8, dot, live), windowButtons);
    right.setAlignment(Pos.CENTER_RIGHT);
    HBox.setMargin(windowButtons, new Insets(0, -20, 0, 14));

    HBox bar = Ui.row(0, Ui.label("SLAGTERI", "brand"), Ui.spacer(), right);
    bar.getStyleClass().add("top-bar");
    bar.setAlignment(Pos.CENTER_LEFT);

    WindowChrome.makeDraggable(bar, stage);

    return bar;
  }

  private VBox buildForm(Consumer<Session> onLogin)
  {
    Button login = Ui.primary("Log ind");
    login.setOnAction(event -> attempt(onLogin));

    username.setOnAction(event -> password.requestFocus());
    password.setOnAction(event -> attempt(onLogin));

    error.setVisible(false);
    error.setManaged(false);

    VBox form = Ui.column(20,
                          Ui.labelled("Brugernavn", username),
                          Ui.labelled("Kodeord", password),
                          login,
                          error);

    form.setPrefWidth(FORM_WIDTH);
    form.setMaxWidth(FORM_WIDTH);

    Label hintOne = Ui.label("station1 · station2 · station3 — indtastning på egen station",
                             "login-hint");
    Label hintTwo = Ui.label("admin — alle stationer, outbox, sporing", "login-hint");

    VBox hints = Ui.column(6, hintOne, hintTwo);
    hints.setAlignment(Pos.CENTER);

    VBox centre = Ui.column(0,
                            Ui.label("TRACEABILITY SYSTEM", "login-mark"),
                            Ui.label("Log ind", "login-title"),
                            form,
                            hints);

    centre.setAlignment(Pos.CENTER);
    centre.setFillWidth(false);

    VBox.setMargin(centre.getChildren().get(1), new javafx.geometry.Insets(12, 0, 38, 0));
    VBox.setMargin(hints, new javafx.geometry.Insets(34, 0, 0, 0));

    return centre;
  }

  private void attempt(Consumer<Session> onLogin)
  {
    Session.authenticate(username.getText(), password.getText())
           .ifPresentOrElse(session ->
                            {
                              hideError();
                              password.clear();
                              onLogin.accept(session);
                            },
                            () -> showError("Forkert brugernavn eller kodeord"));
  }

  private void showError(String message)
  {
    error.setText(message);
    error.setVisible(true);
    error.setManaged(true);
  }

  private void hideError()
  {
    error.setVisible(false);
    error.setManaged(false);
  }
}
