package com.example.desktop.ui;

import com.example.desktop.api.Apis;
import com.example.desktop.domain.Ids;
import com.example.desktop.dto.AnimalDTO;
import com.example.desktop.dto.AnimalPartDTO;
import com.example.desktop.dto.ProductDTO;
import com.example.desktop.dto.TrayDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;

public class AdminOverviewView implements View
{
  private final Apis apis;
  private final VBox root = Ui.column(0);

  private final Label animalCount = tileValue();
  private final Label partCount = tileValue();
  private final Label trayCount = tileValue();
  private final Label productCount = tileValue();
  private final Label trayDetail = Ui.label("", "muted");

  private final ObservableList<AnimalDTO> animals = FXCollections.observableArrayList();
  private final Notice notice = new Notice();

  public AdminOverviewView(Apis apis)
  {
    this.apis = apis;

    HBox tiles = Ui.row(12,
                        tile("Dyr", animalCount, Ui.label("", "muted")),
                        tile("Dele", partCount, Ui.label("", "muted")),
                        tile("Bakker", trayCount, trayDetail),
                        tile("Produkter", productCount, Ui.label("", "muted")));

    tiles.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

    TableView<AnimalDTO> table = animalTable();

    root.getStyleClass().add("main");
    root.getChildren().addAll(tiles, Ui.kick("Dyr — Station 1"), table, Ui.spacer(), notice, hint());
    root.setSpacing(14);

    VBox.setMargin(tiles, new javafx.geometry.Insets(0, 0, 12, 0));
    VBox.setVgrow(table, Priority.ALWAYS);
  }

  @Override
  public String title()
  {
    return "Admin — Oversigt";
  }

  @Override
  public Node node()
  {
    return root;
  }

  @Override
  public void onShown()
  {
    Async.run(() -> apis.registration().animals(), this::applyAnimals, notice::showError);
    Async.run(() -> apis.cutting().parts(), this::applyParts, notice::showError);
    Async.run(() -> apis.cutting().trays(), this::applyTrays, notice::showError);
    Async.run(() -> apis.packing().products(), this::applyProducts, notice::showError);
  }

  private void applyAnimals(List<AnimalDTO> loaded)
  {
    animalCount.setText(String.valueOf(loaded.size()));
    animals.setAll(loaded.stream()
                         .sorted(Comparator.comparing(AnimalDTO::registrationNumber).reversed())
                         .toList());
  }

  private void applyParts(List<AnimalPartDTO> loaded)
  {
    partCount.setText(String.valueOf(loaded.size()));
  }

  private void applyTrays(List<TrayDTO> loaded)
  {
    trayCount.setText(String.valueOf(loaded.size()));

    long full = loaded.stream().filter(tray -> tray.fillRatio() >= 0.8).count();
    trayDetail.setText(full + " næsten fyldte");
  }

  private void applyProducts(List<ProductDTO> loaded)
  {
    productCount.setText(String.valueOf(loaded.size()));
  }

  private TableView<AnimalDTO> animalTable()
  {
    TableView<AnimalDTO> table = new TableView<>(animals);
    table.setPlaceholder(Ui.label("Ingen dyr registreret", "muted"));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    TableColumn<AnimalDTO, String> id = new TableColumn<>("ID");
    id.setCellValueFactory(row ->
        new SimpleStringProperty(Ids.animalLabel(row.getValue().registrationNumber())));

    TableColumn<AnimalDTO, String> origin = new TableColumn<>("OPRINDELSE");
    origin.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().origin()));

    TableColumn<AnimalDTO, String> weight = new TableColumn<>("VÆGT");
    weight.setCellValueFactory(row ->
        new SimpleStringProperty(Ui.kilos(row.getValue().weight())));

    TableColumn<AnimalDTO, String> arrival = new TableColumn<>("ANKOMST");
    arrival.setCellValueFactory(row ->
        new SimpleStringProperty(Ui.dateTime(row.getValue().arrivalDateTime())));

    table.getColumns().addAll(List.of(id, origin, weight, arrival));

    return table;
  }

  private Node tile(String caption, Label value, Label detail)
  {
    VBox box = Ui.column(6, Ui.kick(caption), value, detail);
    box.getStyleClass().add("tile");
    return box;
  }

  private static Label tileValue()
  {
    return Ui.label("—", "mono", "tile-value");
  }

  private Node hint()
  {
    Label title = Ui.label("Skrivebeskyttet", "notice-hint-title");

    Label body = Ui.label("Rettelser og sletninger kræver nye endepunkter på stationerne. "
                          + "En rettelse der ville overfylde en bakke eller bryde deltypen skal "
                          + "afvises med 409 — også for admin.", "notice-hint");
    body.setWrapText(true);

    VBox foot = Ui.column(5, title, body);
    foot.getStyleClass().add("foot");

    return foot;
  }
}
