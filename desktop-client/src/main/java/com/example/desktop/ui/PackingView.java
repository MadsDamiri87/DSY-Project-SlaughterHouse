package com.example.desktop.ui;

import com.example.desktop.api.Apis;
import com.example.desktop.api.CuttingApi;
import com.example.desktop.api.PackingApi;
import com.example.desktop.domain.Ids;
import com.example.desktop.domain.ProductType;
import com.example.desktop.dto.ProductDTO;
import com.example.desktop.dto.TrayDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PackingView implements View
{
  private final PackingApi api;
  private final CuttingApi cuttingApi;
  private final BorderPane root = new BorderPane();

  private final TextField idField = Ui.locked("");
  private final TextField weightField = Ui.field("17,5");
  private final ComboBox<ProductType> typeBox = new ComboBox<>();
  private final TextField search = Ui.field("Skriv for at filtrere");
  private final VBox trayList = Ui.column(8);
  private final Button pack = Ui.primary("Pak produkt");
  private final Notice notice = new Notice();
  private final ObservableList<ProductDTO> recent = FXCollections.observableArrayList();

  private final Set<String> selected = new LinkedHashSet<>();
  private List<TrayDTO> trays = List.of();
  private String nextProductId = Ids.format(Ids.PRODUCT_PREFIX, 1);

  public PackingView(Apis apis)
  {
    this.api = apis.packing();
    this.cuttingApi = apis.cutting();

    typeBox.getItems().setAll(ProductType.all());
    typeBox.setMaxWidth(Double.MAX_VALUE);
    typeBox.setPromptText("Vælg produkttype");

    search.textProperty().addListener((observable, was, is) -> renderTrays());

    root.setCenter(buildMain());
    root.setRight(buildSide());

    updatePackLabel();
  }

  @Override
  public String title()
  {
    return "Station 03 — Pakning";
  }

  @Override
  public Node node()
  {
    return root;
  }

  @Override
  public void onShown()
  {
    reload();
  }

  private Node buildMain()
  {
    pack.setOnAction(event -> submit());

    Node scrollingTrays = Ui.scroll(trayList);
    VBox.setVgrow(scrollingTrays, Priority.ALWAYS);

    VBox main = Ui.column(18,
                          Ui.kick("Pak produkt"),
                          Ui.labelled("Produkt-id", idField),
                          Ui.labelled("Produkttype", typeBox),
                          Ui.labelled("Samlet vægt (kg)", weightField),
                          Ui.kick("Pakket fra bakker"),
                          search,
                          scrollingTrays,
                          pack);

    main.getStyleClass().add("main");
    main.getChildren().addAll(Ui.spacer(), notice);

    return main;
  }

  private Node buildSide()
  {
    TableView<ProductDTO> table = new TableView<>(recent);
    table.setPlaceholder(Ui.label("Ingen produkter endnu", "muted"));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    TableColumn<ProductDTO, String> id = new TableColumn<>("PRODUKT");
    id.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().productId()));

    TableColumn<ProductDTO, String> type = new TableColumn<>("TYPE");
    type.setCellValueFactory(row -> new SimpleStringProperty(readable(row.getValue().productType())));

    TableColumn<ProductDTO, String> weight = new TableColumn<>("KG");
    weight.setCellValueFactory(row -> new SimpleStringProperty(Ui.number(row.getValue().totalWeight())));

    table.getColumns().addAll(List.of(id, type, weight));

    VBox side = Ui.column(14, Ui.kick("Seneste produkter"), table);
    side.getStyleClass().add("side");
    VBox.setVgrow(table, Priority.ALWAYS);

    return side;
  }

  private void reload()
  {
    Async.run(cuttingApi::trays, this::applyTrays, notice::showError);
    Async.run(api::products, this::applyProducts, notice::showError);
  }

  private void applyTrays(List<TrayDTO> loaded)
  {
    trays = loaded.stream().sorted(Comparator.comparing(TrayDTO::trayId)).toList();
    selected.retainAll(trays.stream().map(TrayDTO::trayId).toList());
    renderTrays();
  }

  private void applyProducts(List<ProductDTO> products)
  {
    nextProductId = Ids.next(products.stream().map(ProductDTO::productId).toList(),
                             Ids.PRODUCT_PREFIX);
    idField.setText(nextProductId);

    recent.setAll(products.stream()
                          .sorted(Comparator.comparing(ProductDTO::productId).reversed())
                          .limit(12)
                          .toList());
  }

  private void renderTrays()
  {
    String filter = search.getText() == null ? "" : search.getText().trim().toLowerCase();

    trayList.getChildren().setAll(trays.stream()
                                       .filter(tray -> matches(tray, filter))
                                       .map(this::trayRow)
                                       .toList());

    updatePackLabel();
  }

  private boolean matches(TrayDTO tray, String filter)
  {
    if (filter.isEmpty())
    {
      return true;
    }

    return tray.trayId().toLowerCase().contains(filter)
           || tray.partType().toLowerCase().contains(filter);
  }

  private Node trayRow(TrayDTO tray)
  {
    CheckBox box = new CheckBox();
    box.setSelected(selected.contains(tray.trayId()));
    box.setMouseTransparent(true);
    box.setFocusTraversable(false);

    Label id = Ui.label(tray.trayId(), "tray-id");
    Label meta = Ui.label(tray.partType().toLowerCase() + " · " + Ui.kilos(tray.currentWeight()),
                          "tray-meta");

    HBox row = Ui.row(12, box, id, Ui.spacer(), meta);
    row.getStyleClass().add("tray-cell");

    if (box.isSelected())
    {
      row.getStyleClass().add("tray-cell-on");
    }

    box.selectedProperty().addListener((observable, was, is) ->
    {
      if (is)
      {
        selected.add(tray.trayId());
        row.getStyleClass().add("tray-cell-on");
      }
      else
      {
        selected.remove(tray.trayId());
        row.getStyleClass().remove("tray-cell-on");
      }

      suggestWeight();
      updatePackLabel();
    });

    row.setOnMouseClicked(event -> box.setSelected(!box.isSelected()));

    return row;
  }

  private void suggestWeight()
  {
    double total = trays.stream()
                        .filter(tray -> selected.contains(tray.trayId()))
                        .mapToDouble(TrayDTO::currentWeight)
                        .sum();

    weightField.setText(Ui.number(total));
  }

  private void updatePackLabel()
  {
    pack.setText(selected.isEmpty()
                 ? "PAK PRODUKT"
                 : "PAK PRODUKT · " + selected.size() + " BAKKER VALGT");

    pack.setDisable(selected.isEmpty());
  }

  private void submit()
  {
    ProductType type = typeBox.getValue();
    Double weight = parseWeight(weightField.getText());

    if (type == null)
    {
      notice.showError("Produkttype mangler", "Vælg hvilken slags pakke der er pakket");
      return;
    }

    if (weight == null)
    {
      notice.showError("Ugyldig vægt", "Skriv vægten som et positivt tal, for eksempel 17,5");
      return;
    }

    if (selected.isEmpty())
    {
      notice.showError("Ingen bakker valgt", "Et produkt skal pakkes fra mindst én bakke");
      return;
    }

    ProductDTO product = new ProductDTO(nextProductId,
                                        type.name(),
                                        null,
                                        weight,
                                        List.copyOf(selected));

    Async.run(() -> api.pack(product),
              packed ->
              {
                notice.showReceipt("201 Created", packed.productId());
                selected.clear();
                weightField.clear();
                reload();
              },
              notice::showError);
  }

  private String readable(String constant)
  {
    if (constant == null || constant.isBlank())
    {
      return "";
    }

    String lower = constant.replace('_', ' ').toLowerCase();
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }

  private Double parseWeight(String text)
  {
    if (text == null || text.isBlank())
    {
      return null;
    }

    try
    {
      double value = Double.parseDouble(text.trim().replace(',', '.').replace(" kg", ""));
      return value > 0 ? value : null;
    }
    catch (NumberFormatException exception)
    {
      return null;
    }
  }
}
