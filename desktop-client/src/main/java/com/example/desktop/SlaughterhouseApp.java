package com.example.desktop;

import com.example.desktop.api.Apis;
import com.example.desktop.auth.Session;
import com.example.desktop.domain.Config;
import com.example.desktop.ui.Async;
import com.example.desktop.ui.Health;
import com.example.desktop.ui.LoginView;
import com.example.desktop.ui.MainWindow;
import com.example.desktop.ui.WindowChrome;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SlaughterhouseApp extends Application
{
  private static final double DEFAULT_WIDTH = 1280;
  private static final double DEFAULT_HEIGHT = 800;
  private static final double MIN_WIDTH = 1100;
  private static final double MIN_HEIGHT = 720;

  private Apis apis;
  private Health health;
  private Scene scene;
  private Stage stage;

  @Override
  public void start(Stage stage)
  {
    this.stage = stage;
    apis = new Apis(Config.fromEnvironment());

    scene = new Scene(new javafx.scene.layout.StackPane(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
    scene.setFill(Color.TRANSPARENT);
    scene.getStylesheets().add(stylesheet());

    showLogin();

    stage.initStyle(StageStyle.TRANSPARENT);
    stage.setTitle("Slagteri — Traceability");
    stage.setScene(scene);
    stage.setMinWidth(MIN_WIDTH);
    stage.setMinHeight(MIN_HEIGHT);

    WindowChrome.makeResizable(stage, scene);

    stage.show();
  }

  @Override
  public void stop()
  {
    stopHealth();
    Async.shutdown();

    if (apis != null)
    {
      apis.close();
    }
  }

  private void showLogin()
  {
    stopHealth();

    LoginView login = new LoginView(stage, this::showMain);
    scene.setRoot(login);
    login.focusUsername();
  }

  private void showMain(Session session)
  {
    health = new Health(apis);
    health.start();

    scene.setRoot(new MainWindow(apis, session, health, stage, this::showLogin));
  }

  private void stopHealth()
  {
    if (health != null)
    {
      health.stop();
      health = null;
    }
  }

  private String stylesheet()
  {
    return SlaughterhouseApp.class.getResource("/styles.css").toExternalForm();
  }
}
