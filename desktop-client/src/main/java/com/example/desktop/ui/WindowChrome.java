package com.example.desktop.ui;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public final class WindowChrome
{
  private static final double EDGE = 6;
  private static final double GLYPH = 10;

  private WindowChrome()
  {
  }

  public static void markAsWindow(Node root, Stage stage)
  {
    root.getStyleClass().add("window");

    stage.maximizedProperty().addListener((observable, was, is) ->
    {
      root.getStyleClass().remove("window-maximised");

      if (is)
      {
        root.getStyleClass().add("window-maximised");
      }
    });

    if (stage.isMaximized())
    {
      root.getStyleClass().add("window-maximised");
    }
  }

  public static HBox closeOnly(Stage stage)
  {
    Button close = button(closeGlyph(), "win-button", "win-close");
    close.setOnAction(event -> stage.close());

    return chrome(close);
  }

  public static HBox buttons(Stage stage)
  {
    Button minimise = button(minimiseGlyph(), "win-button");
    minimise.setOnAction(event -> stage.setIconified(true));

    Button maximise = button(maximiseGlyph(), "win-button");
    maximise.setOnAction(event -> toggleMaximised(stage));

    Button close = button(closeGlyph(), "win-button", "win-close");
    close.setOnAction(event -> Platform.exit());

    return chrome(minimise, maximise, close);
  }

  private static HBox chrome(Button... buttons)
  {
    HBox box = new HBox(0, buttons);
    box.getStyleClass().add("win-buttons");

    box.addEventHandler(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);
    box.addEventHandler(MouseEvent.MOUSE_DRAGGED, MouseEvent::consume);
    box.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

    return box;
  }

  public static void makeDraggable(Node bar, Stage stage)
  {
    double[] offset = new double[2];

    bar.setOnMousePressed(event ->
    {
      offset[0] = event.getScreenX() - stage.getX();
      offset[1] = event.getScreenY() - stage.getY();
    });

    bar.setOnMouseDragged(event ->
    {
      if (stage.isMaximized())
      {
        return;
      }

      stage.setX(event.getScreenX() - offset[0]);
      stage.setY(event.getScreenY() - offset[1]);
    });

    bar.setOnMouseClicked(event ->
    {
      if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)
      {
        toggleMaximised(stage);
      }
    });
  }

  public static void makeResizable(Stage stage, Scene scene)
  {
    double[] start = new double[4];
    boolean[] edges = new boolean[4];

    scene.addEventFilter(MouseEvent.MOUSE_MOVED, event ->
    {
      if (stage.isMaximized())
      {
        scene.setCursor(Cursor.DEFAULT);
        return;
      }

      scene.setCursor(cursorFor(scene, event.getSceneX(), event.getSceneY()));
    });

    scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->
    {
      if (stage.isMaximized())
      {
        return;
      }

      edges[0] = event.getSceneX() < EDGE;
      edges[1] = event.getSceneX() > scene.getWidth() - EDGE;
      edges[2] = event.getSceneY() < EDGE;
      edges[3] = event.getSceneY() > scene.getHeight() - EDGE;

      if (resizing(edges))
      {
        start[0] = stage.getX();
        start[1] = stage.getY();
        start[2] = stage.getWidth();
        start[3] = stage.getHeight();
        event.consume();
      }
    });

    scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, event ->
    {
      if (!resizing(edges))
      {
        return;
      }

      if (edges[1])
      {
        stage.setWidth(Math.max(stage.getMinWidth(), event.getScreenX() - stage.getX()));
      }

      if (edges[3])
      {
        stage.setHeight(Math.max(stage.getMinHeight(), event.getScreenY() - stage.getY()));
      }

      if (edges[0])
      {
        double right = start[0] + start[2];
        double width = Math.max(stage.getMinWidth(), right - event.getScreenX());
        stage.setX(right - width);
        stage.setWidth(width);
      }

      if (edges[2])
      {
        double bottom = start[1] + start[3];
        double height = Math.max(stage.getMinHeight(), bottom - event.getScreenY());
        stage.setY(bottom - height);
        stage.setHeight(height);
      }

      event.consume();
    });

    scene.addEventFilter(MouseEvent.MOUSE_RELEASED, event ->
    {
      edges[0] = false;
      edges[1] = false;
      edges[2] = false;
      edges[3] = false;
    });
  }

  private static boolean resizing(boolean[] edges)
  {
    return edges[0] || edges[1] || edges[2] || edges[3];
  }

  private static Cursor cursorFor(Scene scene, double x, double y)
  {
    boolean left = x < EDGE;
    boolean right = x > scene.getWidth() - EDGE;
    boolean top = y < EDGE;
    boolean bottom = y > scene.getHeight() - EDGE;

    if (top && left)     { return Cursor.NW_RESIZE; }
    if (top && right)    { return Cursor.NE_RESIZE; }
    if (bottom && left)  { return Cursor.SW_RESIZE; }
    if (bottom && right) { return Cursor.SE_RESIZE; }
    if (left || right)   { return Cursor.H_RESIZE; }
    if (top || bottom)   { return Cursor.V_RESIZE; }

    return Cursor.DEFAULT;
  }

  private static void toggleMaximised(Stage stage)
  {
    stage.setMaximized(!stage.isMaximized());
  }

  private static Button button(Node glyph, String... styleClasses)
  {
    Button button = new Button();
    button.setGraphic(glyph);
    button.getStyleClass().addAll(styleClasses);
    button.setFocusTraversable(false);
    return button;
  }

  private static Node minimiseGlyph()
  {
    Line line = new Line(0, 0, GLYPH, 0);
    line.getStyleClass().add("win-glyph");
    return wrap(line);
  }

  private static Node maximiseGlyph()
  {
    Rectangle rectangle = new Rectangle(GLYPH, GLYPH);
    rectangle.setFill(null);
    rectangle.getStyleClass().add("win-glyph");
    return wrap(rectangle);
  }

  private static Node closeGlyph()
  {
    Line one = new Line(0, 0, GLYPH, GLYPH);
    Line two = new Line(0, GLYPH, GLYPH, 0);
    one.getStyleClass().add("win-glyph");
    two.getStyleClass().add("win-glyph");

    StackPane stack = new StackPane(one, two);
    stack.setPrefSize(GLYPH, GLYPH);
    stack.setMinSize(GLYPH, GLYPH);
    return stack;
  }

  private static Pane wrap(Node glyph)
  {
    StackPane stack = new StackPane(glyph);
    stack.setPrefSize(GLYPH, GLYPH);
    stack.setMinSize(GLYPH, GLYPH);
    return stack;
  }
}
