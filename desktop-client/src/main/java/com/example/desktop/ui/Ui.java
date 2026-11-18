package com.example.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Ui
{
  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd-MM HH:mm").withZone(ZoneId.systemDefault());

  private Ui()
  {
  }

  public static Label label(String text, String... styleClasses)
  {
    Label label = new Label(text);
    label.getStyleClass().addAll(styleClasses);
    return label;
  }

  public static Label kick(String text)
  {
    return label(text.toUpperCase(Locale.ROOT), "kick");
  }

  public static Label fieldLabel(String text)
  {
    return label(text.toUpperCase(Locale.ROOT), "field-label");
  }

  public static TextField field(String prompt)
  {
    TextField field = new TextField();
    field.setPromptText(prompt);
    return field;
  }

  public static TextField locked(String value)
  {
    TextField field = new TextField(value);
    field.setEditable(false);
    field.setFocusTraversable(false);
    field.getStyleClass().add("text-field-locked");
    return field;
  }

  public static Button primary(String text)
  {
    Button button = new Button(text.toUpperCase(Locale.ROOT));
    button.getStyleClass().add("primary-button");
    button.setMaxWidth(Double.MAX_VALUE);
    return button;
  }

  public static Button ghost(String text)
  {
    Button button = new Button(text.toUpperCase(Locale.ROOT));
    button.getStyleClass().add("ghost-button");
    button.setMaxWidth(Double.MAX_VALUE);
    return button;
  }

  public static Region spacer()
  {
    Region region = new Region();
    HBox.setHgrow(region, Priority.ALWAYS);
    VBox.setVgrow(region, Priority.ALWAYS);
    return region;
  }

  public static VBox column(double spacing, Node... children)
  {
    VBox box = new VBox(spacing, children);
    box.setFillWidth(true);
    return box;
  }

  public static HBox row(double spacing, Node... children)
  {
    HBox box = new HBox(spacing, children);
    box.setAlignment(Pos.CENTER_LEFT);
    return box;
  }

  public static HBox field(String labelText, Node control)
  {
    HBox box = row(10, control);
    HBox.setHgrow(control, Priority.ALWAYS);
    box.getChildren().add(0, fieldLabel(labelText));
    return box;
  }

  public static VBox labelled(String labelText, Node control)
  {
    VBox box = column(7, fieldLabel(labelText), control);
    if (control instanceof Region region)
    {
      region.setMaxWidth(Double.MAX_VALUE);
    }
    return box;
  }

  public static Circle dot(double radius)
  {
    return new Circle(radius);
  }

  public static ScrollPane scroll(Node content)
  {
    ScrollPane pane = new ScrollPane(content);
    pane.setFitToWidth(true);
    pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    pane.setPadding(Insets.EMPTY);
    return pane;
  }

  public static String time(Instant instant)
  {
    return instant == null ? "" : TIME.format(instant);
  }

  public static String dateTime(Instant instant)
  {
    return instant == null ? "" : DATE_TIME.format(instant);
  }

  public static String kilos(double value)
  {
    return String.format(Locale.of("da", "DK"), "%.1f kg", value);
  }

  public static String number(double value)
  {
    return String.format(Locale.of("da", "DK"), "%.1f", value);
  }
}
