package com.example.desktop.ui;

import com.example.desktop.api.Apis;
import com.example.desktop.api.TraceApi;
import com.example.desktop.domain.Ids;
import generated.GetAnimalTraceResponse;
import generated.GetPartTraceResponse;
import generated.TracedPart;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class TraceView implements View
{
  private enum Mode
  {
    PRODUCT("Produkt", "GetAnimalsForProduct"),
    ANIMAL("Dyr", "GetAnimalTrace"),
    PART("Del", "GetPartTrace");

    private final String label;
    private final String rpc;

    Mode(String label, String rpc)
    {
      this.label = label;
      this.rpc = rpc;
    }
  }

  private final TraceApi api;
  private final VBox root = Ui.column(0);

  private final TextField query = Ui.field("PROD-00050");
  private final ToggleGroup modes = new ToggleGroup();
  private final Label meta = Ui.label("", "muted");
  private final TreeView<String> tree = new TreeView<>();
  private final Notice notice = new Notice();
  private final VBox foot = Ui.column(5);
  private final Label footTitle = Ui.label("", "notice-hint-title");

  public TraceView(Apis apis)
  {
    this.api = apis.trace();

    tree.setShowRoot(true);
    tree.setFocusTraversable(false);

    Label explanation = Ui.label("Et produkt kender sine bakker, men ikke hvilken enkelt del "
                                 + "der endte i hvilken pakke — derfor er sporingen en bevidst "
                                 + "overvurdering.", "notice-hint");
    explanation.setWrapText(true);

    foot.getStyleClass().add("foot");
    foot.getChildren().addAll(footTitle, explanation);
    hideFoot();

    root.getStyleClass().add("main");
    root.setSpacing(0);
    root.getChildren().addAll(buildSearchRow(), meta, tree, notice, foot);

    VBox.setMargin(meta, new javafx.geometry.Insets(8, 0, 20, 0));
    VBox.setVgrow(tree, Priority.ALWAYS);
  }

  @Override
  public String title()
  {
    return "Sporing";
  }

  @Override
  public Node node()
  {
    return root;
  }

  private Node buildSearchRow()
  {
    HBox segment = new HBox();

    for (Mode mode : Mode.values())
    {
      ToggleButton button = new ToggleButton(mode.label);
      button.setToggleGroup(modes);
      button.setUserData(mode);
      button.setOnAction(event -> button.setSelected(true));
      segment.getChildren().add(button);
    }

    ((ToggleButton) segment.getChildren().get(0)).setSelected(true);

    var lookup = Ui.primary("Slå op");
    lookup.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    lookup.setOnAction(event -> search());

    query.setOnAction(event -> search());

    HBox row = Ui.row(12, segment, query, lookup);
    HBox.setHgrow(query, Priority.ALWAYS);

    return row;
  }

  private Mode selectedMode()
  {
    ToggleButton selected = (ToggleButton) modes.getSelectedToggle();
    return selected == null ? Mode.PRODUCT : (Mode) selected.getUserData();
  }

  private void search()
  {
    String text = query.getText() == null ? "" : query.getText().trim();

    if (text.isEmpty())
    {
      notice.showError("Intet at slå op", "Skriv et id i feltet");
      return;
    }

    Mode mode = selectedMode();
    notice.clear();
    hideFoot();

    long started = System.nanoTime();

    switch (mode)
    {
      case PRODUCT -> Async.run(() -> api.animalsForProduct(text),
                                animals -> showProduct(text, animals, elapsed(started)),
                                this::fail);

      case ANIMAL -> Async.run(() -> api.animalTrace(registrationNumberOf(text)),
                               trace -> showAnimal(trace, elapsed(started)),
                               this::fail);

      case PART -> Async.run(() -> api.partTrace(text.toUpperCase()),
                             trace -> showPart(trace, elapsed(started)),
                             this::fail);
    }
  }

  private void showProduct(String productId, List<Integer> animals, long millis)
  {
    TreeItem<String> root = new TreeItem<>(productId);
    root.setExpanded(true);

    for (Integer number : animals)
    {
      root.getChildren().add(new TreeItem<>(Ids.animalLabel(number)));
    }

    if (animals.isEmpty())
    {
      root.getChildren().add(new TreeItem<>("ingen dyr fundet"));
    }

    tree.setRoot(root);
    meta.setText(Mode.PRODUCT.rpc + " · gRPC · " + millis + " ms");

    showFoot(animals.size() + " dyr kan indgå i dette produkt.");
  }

  private void showAnimal(GetAnimalTraceResponse trace, long millis)
  {
    String species = trace.getSpecies().isBlank() ? "ukendt art" : trace.getSpecies();

    TreeItem<String> root = new TreeItem<>(
        Ids.animalLabel(trace.getRegistrationNumber())
        + "  ·  " + species
        + "  ·  " + trace.getOrigin()
        + "  ·  " + Ui.number(trace.getWeight()) + " kg");

    root.setExpanded(true);

    for (TracedPart part : trace.getPartsList())
    {
      root.getChildren().add(partItem(part));
    }

    if (trace.getPartsList().isEmpty())
    {
      root.getChildren().add(new TreeItem<>("ingen dele modtaget for dette dyr endnu"));
    }

    tree.setRoot(root);
    meta.setText(Mode.ANIMAL.rpc + " · gRPC · " + millis + " ms");

    long packed = trace.getPartsList().stream()
                       .filter(part -> part.getProductIdsCount() > 0)
                       .count();

    if (packed > 0)
    {
      showFoot(packed + " af " + trace.getPartsCount()
               + " dele ligger i en bakke der kan indgå i produkter.");
    }
  }

  private void showPart(GetPartTraceResponse trace, long millis)
  {
    String species = trace.getSpecies().isBlank() ? "ukendt art" : trace.getSpecies();

    TreeItem<String> root = new TreeItem<>(trace.getPart().getPartId()
                                           + "  ·  " + trace.getPart().getType().toLowerCase()
                                           + "  ·  " + Ui.number(trace.getPart().getWeight()) + " kg");
    root.setExpanded(true);

    TreeItem<String> animal = new TreeItem<>("stammer fra  "
                                             + Ids.animalLabel(trace.getRegistrationNumber())
                                             + "  ·  " + species + "  ·  " + trace.getOrigin());
    animal.setExpanded(true);

    root.getChildren().add(animal);
    root.getChildren().add(trayItem(trace.getPart()));

    tree.setRoot(root);
    meta.setText(Mode.PART.rpc + " · gRPC · " + millis + " ms");

    if (trace.getPart().getProductIdsCount() > 0)
    {
      showFoot(trace.getPart().getProductIdsCount() + " produkter kan indeholde denne del.");
    }
  }

  private TreeItem<String> partItem(TracedPart part)
  {
    TreeItem<String> item = new TreeItem<>(part.getPartId()
                                           + "  ·  " + part.getType().toLowerCase()
                                           + "  ·  " + Ui.number(part.getWeight()) + " kg");
    item.setExpanded(true);
    item.getChildren().add(trayItem(part));

    return item;
  }

  private TreeItem<String> trayItem(TracedPart part)
  {
    if (part.getTrayId().isBlank())
    {
      return new TreeItem<>("ikke lagt i en bakke endnu");
    }

    TreeItem<String> tray = new TreeItem<>("bakke  " + part.getTrayId());
    tray.setExpanded(true);

    if (part.getProductIdsCount() == 0)
    {
      tray.getChildren().add(new TreeItem<>("ingen produkter modtaget for denne bakke endnu"));
      return tray;
    }

    for (String productId : part.getProductIdsList())
    {
      tray.getChildren().add(new TreeItem<>("kan være i  " + productId));
    }

    return tray;
  }

  private int registrationNumberOf(String text)
  {
    String digits = text.toUpperCase().replace(Ids.ANIMAL_PREFIX, "").trim();

    try
    {
      return Integer.parseInt(digits);
    }
    catch (NumberFormatException exception)
    {
      throw new IllegalArgumentException("Skriv et dyrenummer, for eksempel 17 eller ANIMAL-00017");
    }
  }

  private void fail(Throwable failure)
  {
    tree.setRoot(null);
    meta.setText("");
    hideFoot();
    notice.showError(failure);
  }

  private long elapsed(long startedNanos)
  {
    return (System.nanoTime() - startedNanos) / 1_000_000;
  }

  private void showFoot(String text)
  {
    footTitle.setText(text);
    foot.setVisible(true);
    foot.setManaged(true);
  }

  private void hideFoot()
  {
    foot.setVisible(false);
    foot.setManaged(false);
  }
}
