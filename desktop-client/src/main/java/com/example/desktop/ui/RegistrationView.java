package com.example.desktop.ui;

import com.example.desktop.api.Apis;
import com.example.desktop.api.RegistrationApi;
import com.example.desktop.domain.Ids;
import com.example.desktop.dto.AnimalDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class RegistrationView implements View
{
  private final RegistrationApi api;
  private final BorderPane root = new BorderPane();

  private final TextField idField = Ui.locked("");
  private final TextField weightField = Ui.field("97,5");
  private final ComboBox<String> originBox = new ComboBox<>();
  private final Notice notice = new Notice();
  private final ObservableList<AnimalDTO> recent = FXCollections.observableArrayList();

  private int nextNumber = 1;

  public RegistrationView(Apis apis)
  {
    this.api = apis.registration();

    originBox.setEditable(true);
    originBox.setMaxWidth(Double.MAX_VALUE);
    originBox.setPromptText("Vælg eller skriv gård");

    weightField.setOnAction(event -> submit());

    root.setCenter(buildMain());
    root.setRight(buildSide());
  }

  @Override
  public String title()
  {
    return "Station 01 — Registrering";
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
    var register = Ui.primary("Registrér dyr");
    register.setOnAction(event -> submit());

    VBox main = Ui.column(18,
                          Ui.kick("Modtag dyr"),
                          Ui.labelled("Dyrenummer", idField),
                          Ui.labelled("Vægt (kg)", weightField),
                          Ui.labelled("Oprindelse", originBox),
                          register);

    main.getStyleClass().add("main");
    main.getChildren().addAll(Ui.spacer(), notice);

    return main;
  }

  private Node buildSide()
  {
    TableView<AnimalDTO> table = new TableView<>(recent);
    table.setPlaceholder(Ui.label("Ingen dyr endnu", "muted"));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    TableColumn<AnimalDTO, String> id = new TableColumn<>("DYR");
    id.setCellValueFactory(row ->
        new SimpleStringProperty(Ids.animalLabel(row.getValue().registrationNumber())));

    TableColumn<AnimalDTO, String> weight = new TableColumn<>("KG");
    weight.setCellValueFactory(row ->
        new SimpleStringProperty(Ui.number(row.getValue().weight())));

    TableColumn<AnimalDTO, String> arrival = new TableColumn<>("ANKOMST");
    arrival.setCellValueFactory(row ->
        new SimpleStringProperty(Ui.time(row.getValue().arrivalDateTime())));

    table.getColumns().addAll(List.of(id, weight, arrival));

    VBox side = Ui.column(14, Ui.kick("Seneste"), table);
    side.getStyleClass().add("side");
    VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

    return side;
  }

  private void reload()
  {
    Async.run(api::animals, this::apply, notice::showError);
  }

  private void apply(List<AnimalDTO> animals)
  {
    nextNumber = Ids.nextRegistrationNumber(animals.stream()
                                                   .map(AnimalDTO::registrationNumber)
                                                   .toList());

    idField.setText(Ids.animalLabel(nextNumber));

    String selected = originBox.getEditor().getText();

    originBox.getItems().setAll(new TreeSet<>(animals.stream()
                                                     .map(AnimalDTO::origin)
                                                     .filter(origin -> origin != null && !origin.isBlank())
                                                     .toList()));

    originBox.getEditor().setText(selected);

    recent.setAll(animals.stream()
                         .sorted(Comparator.comparing(AnimalDTO::registrationNumber).reversed())
                         .limit(12)
                         .toList());
  }

  private void submit()
  {
    Double weight = parseWeight(weightField.getText());
    String origin = originBox.getEditor().getText();

    if (weight == null)
    {
      notice.showError("Ugyldig vægt", "Skriv vægten som et positivt tal, for eksempel 97,5");
      return;
    }

    if (origin == null || origin.isBlank())
    {
      notice.showError("Oprindelse mangler", "Vælg en gård i listen eller skriv en ny");
      return;
    }

    AnimalDTO animal = new AnimalDTO(nextNumber, null, weight, origin.trim());

    Async.run(() -> api.register(animal),
              registered ->
              {
                notice.showReceipt("201 Created", Ids.animalLabel(registered.registrationNumber()));
                weightField.clear();
                reload();
              },
              notice::showError);
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
