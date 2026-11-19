package com.example.desktop.ui;

import com.example.desktop.api.Apis;
import com.example.desktop.auth.Session;
import com.example.desktop.domain.Station;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MainWindow extends BorderPane
{
  private final Session session;
  private final Health health;
  private final Label brand = Ui.label("", "brand");
  private final VBox rail = new VBox();
  private final Map<String, Button> links = new LinkedHashMap<>();
  private final Map<String, View> views = new LinkedHashMap<>();
  private final Map<String, Supplier<View>> factories = new LinkedHashMap<>();

  public MainWindow(Apis apis, Session session, Health health, Stage stage, Runnable onLogout)
  {
    this.session = session;
    this.health = health;

    WindowChrome.markAsWindow(this, stage);
    setTop(buildTopBar(stage, onLogout));

    TraceView trace = new TraceView(apis);
    factories.put("trace", () -> trace);
    factories.put("st1", () -> new RegistrationView(apis));
    factories.put("st2", () -> new CuttingView(apis));
    factories.put("st3", () -> new PackingView(apis));

    if (session.role().isAdmin())
    {
      factories.put("admin-overview", () -> new AdminOverviewView(apis));
      factories.put("admin-outbox", () -> new AdminOutboxView(apis));
    }

    buildRail();
    setLeft(rail);

    show("trace");
  }

  public void show(String key)
  {
    View view = views.computeIfAbsent(key, missing -> factories.get(missing).get());

    brand.setText(view.title().toUpperCase());
    setCenter(view.node());

    links.forEach((linkKey, button) ->
    {
      button.getStyleClass().remove("rail-link-on");
      if (linkKey.equals(key))
      {
        button.getStyleClass().add("rail-link-on");
      }
    });

    view.onShown();
  }

  private HBox buildTopBar(Stage stage, Runnable onLogout)
  {
    Circle dot = Ui.dot(4);
    Label live = Ui.label("", "live");

    Runnable sync = () ->
    {
      boolean up = health.backendUpProperty().get();

      live.setText(up ? "SERVER RASK" : "SERVER SVARER IKKE");
      live.getStyleClass().removeAll("live-up", "live-down");
      live.getStyleClass().add(up ? "live-up" : "live-down");

      dot.getStyleClass().removeAll("dot-up", "dot-down");
      dot.getStyleClass().add(up ? "dot-up" : "dot-down");
    };

    health.backendUpProperty().addListener((observable, was, is) -> sync.run());
    sync.run();

    HBox status = Ui.row(8, dot, live);

    HBox who = Ui.row(9,
                      Ui.label("Logget ind som:", "who"),
                      Ui.label(session.username(), "who-name", "mono"));

    Button logout = new Button("LOG UD");
    logout.getStyleClass().add("logout");
    logout.setOnAction(event -> onLogout.run());

    HBox windowButtons = WindowChrome.buttons(stage);

    HBox right = Ui.row(18, status, who, logout, windowButtons);
    right.setAlignment(Pos.CENTER_RIGHT);
    HBox.setMargin(windowButtons, new Insets(0, -20, 0, 14));

    HBox bar = Ui.row(0, brand, Ui.spacer(), right);
    bar.getStyleClass().add("top-bar");
    bar.setAlignment(Pos.CENTER_LEFT);

    WindowChrome.makeDraggable(bar, stage);

    return bar;
  }

  private void buildRail()
  {
    rail.getStyleClass().add("rail");

    addLink("trace", "Sporing", true);
    addLink("st1", "Station 1", session.role().mayUse(Station.REGISTRATION.role()));
    addLink("st2", "Station 2", session.role().mayUse(Station.CUTTING.role()));
    addLink("st3", "Station 3", session.role().mayUse(Station.PACKING.role()));

    if (session.role().isAdmin())
    {
      rail.getChildren().add(Ui.label("ADMINISTRATION", "rail-head"));
      addLink("admin-overview", "Oversigt", true);
      addLink("admin-outbox", "Outbox", true);
    }

    rail.getChildren().add(Ui.label("OUTBOX", "rail-head"));

    for (Station station : Station.all())
    {
      if (session.role().mayUse(station.role()))
      {
        rail.getChildren().add(queueRow(station));
      }
    }
  }

  private void addLink(String key, String text, boolean enabled)
  {
    Button link = new Button(text);
    link.getStyleClass().add("rail-link");
    link.setMaxWidth(Double.MAX_VALUE);
    link.setAlignment(Pos.CENTER_LEFT);

    if (enabled)
    {
      link.setOnAction(event -> show(key));
      links.put(key, link);
    }
    else
    {
      link.getStyleClass().add("rail-link-off");
      link.setDisable(true);
    }

    rail.getChildren().add(link);
  }

  private HBox queueRow(Station station)
  {
    Label name = Ui.label(station.shortName());
    Label value = Ui.label("", "mono");

    HBox row = Ui.row(0, name, Ui.spacer(), value);
    row.getStyleClass().add("qrow");

    Runnable sync = () ->
    {
      OutboxCount count = health.countProperty(station).get();

      value.setText(count.label());

      row.getStyleClass().removeAll("qrow-quiet", "qrow-alarm", "qrow-bad");
      row.getStyleClass().add(count.styleClass());

      name.getStyleClass().removeAll("qrow-quiet", "qrow-alarm", "qrow-bad");
      name.getStyleClass().add(count.styleClass());

      value.getStyleClass().removeAll("qrow-quiet", "qrow-alarm", "qrow-bad");
      value.getStyleClass().add(count.styleClass());
    };

    health.countProperty(station).addListener((observable, was, is) -> sync.run());
    sync.run();

    return row;
  }
}
