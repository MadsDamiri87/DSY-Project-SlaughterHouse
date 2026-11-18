package com.example.desktop.ui;

import com.example.desktop.api.CuttingApi;
import com.example.desktop.api.Apis;
import com.example.desktop.api.RegistrationApi;
import com.example.desktop.domain.Ids;
import com.example.desktop.domain.PartType;
import com.example.desktop.dto.AnimalDTO;
import com.example.desktop.dto.AnimalPartDTO;
import com.example.desktop.dto.TrayDTO;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CuttingView implements View
{
  private static final TrayDTO NO_TRAY = new TrayDTO("", "", 0, 0);

  private final CuttingApi api;
  private final RegistrationApi registrationApi;
  private final BorderPane root = new BorderPane();

  private final TextField idField = Ui.locked("");
  private final TextField weightField = Ui.field("4,5");
  private final ComboBox<PartType> typeBox = new ComboBox<>();
  private final ComboBox<AnimalDTO> animalBox = new ComboBox<>();
  private final ComboBox<TrayDTO> trayBox = new ComboBox<>();
  private final Notice notice = new Notice();
  private final VBox gauges = Ui.column(0);

  private String nextPartId = Ids.format(Ids.PART_PREFIX, 1);
  private List<TrayDTO> trays = List.of();

  public CuttingView(Apis apis)
  {
    this.api = apis.cutting();
    this.registrationApi = apis.registration();

    typeBox.getItems().setAll(PartType.all());
    typeBox.setMaxWidth(Double.MAX_VALUE);
    typeBox.setPromptText("Vælg deltype");

    animalBox.setMaxWidth(Double.MAX_VALUE);
    animalBox.setPromptText("Vælg dyr");
    animalBox.setConverter(animalConverter());

    trayBox.setMaxWidth(Double.MAX_VALUE);
    trayBox.setConverter(trayConverter());

    weightField.setOnAction(event -> submit());

    root.setCenter(buildMain());
    root.setRight(buildSide());
  }

  @Override
  public String title()
  {
    return "Station 02 — Opskæring";
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
    var register = Ui.primary("Registrér del");
    register.setOnAction(event -> submit());

    var clear = Ui.ghost("Ryd");
    clear.setOnAction(event -> clearForm());

    HBox buttons = Ui.row(14, register, clear);
    HBox.setHgrow(register, Priority.ALWAYS);
    HBox.setHgrow(clear, Priority.ALWAYS);

    VBox main = Ui.column(18,
                          Ui.kick("Registrér del"),
                          Ui.labelled("Del-id", idField),
                          Ui.labelled("Deltype", typeBox),
                          Ui.labelled("Vægt (kg)", weightField),
                          Ui.labelled("Dyr", animalBox),
                          Ui.labelled("Læg i bakke", trayBox),
                          buttons);

    main.getStyleClass().add("main");
    main.getChildren().addAll(Ui.spacer(), notice);

    return main;
  }

  private Node buildSide()
  {
    var newTray = Ui.ghost("Opret ny bakke");
    newTray.setOnAction(event -> createTray());

    VBox side = Ui.column(14, Ui.kick("Bakker"), Ui.scroll(gauges), newTray);
    side.getStyleClass().add("side");
    gauges.setSpacing(22);

    return side;
  }

  private void reload()
  {
    Async.run(api::trays, this::applyTrays, notice::showError);
    Async.run(api::parts, this::applyParts, notice::showError);
    Async.run(registrationApi::animals, this::applyAnimals, failure -> { });
  }

  private void applyParts(List<AnimalPartDTO> parts)
  {
    nextPartId = Ids.next(parts.stream().map(AnimalPartDTO::partId).toList(), Ids.PART_PREFIX);
    idField.setText(nextPartId);
  }

  private void applyAnimals(List<AnimalDTO> animals)
  {
    AnimalDTO selected = animalBox.getValue();

    animalBox.getItems().setAll(animals.stream()
                                       .sorted(Comparator.comparing(AnimalDTO::registrationNumber).reversed())
                                       .toList());

    if (selected != null && animalBox.getItems().contains(selected))
    {
      animalBox.setValue(selected);
    }
  }

  private void applyTrays(List<TrayDTO> loaded)
  {
    trays = loaded;

    TrayDTO selected = trayBox.getValue();

    List<TrayDTO> options = new ArrayList<>();
    options.add(NO_TRAY);
    options.addAll(loaded.stream().sorted(Comparator.comparing(TrayDTO::trayId)).toList());

    trayBox.getItems().setAll(options);
    trayBox.setValue(selected != null && options.contains(selected) ? selected : NO_TRAY);

    gauges.getChildren().setAll(loaded.stream()
                                      .sorted(Comparator.comparing(TrayDTO::trayId))
                                      .map(this::gauge)
                                      .toList());
  }

  private Node gauge(TrayDTO tray)
  {
    Label id = Ui.label(tray.trayId(), "tray-id");
    Label type = Ui.label(tray.partType().toUpperCase(), "tray-meta");

    HBox header = Ui.row(0, id, Ui.spacer(), type);

    ProgressBar bar = new ProgressBar(tray.fillRatio());
    bar.setMaxWidth(Double.MAX_VALUE);
    bar.setPrefHeight(8);

    if (tray.fillRatio() >= 0.8)
    {
      bar.getStyleClass().add("progress-bar-warn");
    }

    Label value = Ui.label(Ui.number(tray.currentWeight()) + " / " + Ui.kilos(tray.maxWeight()),
                           "tray-id");

    return Ui.column(9, header, bar, value);
  }

  private void submit()
  {
    PartType type = typeBox.getValue();
    AnimalDTO animal = animalBox.getValue();
    Double weight = parseWeight(weightField.getText());
    TrayDTO tray = trayBox.getValue();

    if (type == null)
    {
      notice.showError("Deltype mangler", "Vælg hvilken del der er skåret ud");
      return;
    }

    if (animal == null)
    {
      notice.showError("Dyr mangler", "Vælg hvilket dyr delen stammer fra");
      return;
    }

    if (weight == null)
    {
      notice.showError("Ugyldig vægt", "Skriv vægten som et positivt tal, for eksempel 4,5");
      return;
    }

    AnimalPartDTO part = new AnimalPartDTO(nextPartId,
                                           type.name(),
                                           weight,
                                           animal.registrationNumber(),
                                           null,
                                           null);

    boolean placeInTray = tray != null && !tray.trayId().isBlank();
    String trayId = placeInTray ? tray.trayId() : null;

    Async.run(() ->
              {
                AnimalPartDTO registered = api.registerPart(part);
                return placeInTray ? api.placeInTray(registered.partId(), trayId) : registered;
              },
              saved ->
              {
                notice.showReceipt("201 Created",
                                   saved.partId() + (placeInTray ? " · " + trayId : ""));
                weightField.clear();
                reload();
              },
              notice::showError);
  }

  private void createTray()
  {
    ComboBox<PartType> type = new ComboBox<>();
    type.getItems().setAll(PartType.all());
    type.setMaxWidth(Double.MAX_VALUE);

    TextField maxWeight = Ui.field("50,0");

    String trayId = Ids.next(trays.stream().map(TrayDTO::trayId).toList(), Ids.TRAY_PREFIX);

    VBox content = Ui.column(18,
                             Ui.labelled("Bakke-id", Ui.locked(trayId)),
                             Ui.labelled("Deltype", type),
                             Ui.labelled("Maksimal vægt (kg)", maxWeight));

    content.setPrefWidth(380);

    Window owner = root.getScene() == null ? null : root.getScene().getWindow();

    if (!new AppDialog(owner, "Opret ny bakke", content, "Opret").showAndWait())
    {
      return;
    }

    Double limit = parseWeight(maxWeight.getText());

    if (type.getValue() == null || limit == null)
    {
      notice.showError("Bakken blev ikke oprettet", "Deltype og en positiv maksimal vægt er påkrævet");
      return;
    }

    TrayDTO tray = new TrayDTO(trayId, type.getValue().name(), limit, 0);

    Async.run(() -> api.createTray(tray),
              created ->
              {
                notice.showReceipt("201 Created", created.trayId());
                reload();
              },
              notice::showError);
  }

  private void clearForm()
  {
    typeBox.setValue(null);
    animalBox.setValue(null);
    trayBox.setValue(NO_TRAY);
    weightField.clear();
    notice.clear();
  }

  private StringConverter<AnimalDTO> animalConverter()
  {
    return new StringConverter<>()
    {
      @Override
      public String toString(AnimalDTO animal)
      {
        return animal == null ? "" : Ids.animalLabel(animal.registrationNumber())
                                     + "  ·  " + animal.origin();
      }

      @Override
      public AnimalDTO fromString(String text)
      {
        return null;
      }
    };
  }

  private StringConverter<TrayDTO> trayConverter()
  {
    return new StringConverter<>()
    {
      @Override
      public String toString(TrayDTO tray)
      {
        if (tray == null || tray.trayId().isBlank())
        {
          return "Ingen bakke endnu";
        }
        return tray.trayId() + "  ·  " + tray.partType().toLowerCase()
               + "  ·  " + Ui.number(tray.remaining()) + " kg fri";
      }

      @Override
      public TrayDTO fromString(String text)
      {
        return null;
      }
    };
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
