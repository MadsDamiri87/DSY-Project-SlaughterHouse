package com.example.desktop.ui;

import com.example.desktop.api.Apis;
import com.example.desktop.domain.Station;
import com.example.desktop.dto.OutboxEntryDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AdminOutboxView implements View
{
  private record Row(Station station, OutboxEntryDTO entry)
  {
  }

  private enum Filter
  {
    ALL("Alle"),
    PENDING("I kø"),
    FAILED("Fejlet");

    private final String label;

    Filter(String label)
    {
      this.label = label;
    }
  }

  private final Apis apis;
  private final VBox root = Ui.column(14);

  private final ObservableList<Row> rows = FXCollections.observableArrayList();
  private final List<Row> loaded = new ArrayList<>();
  private final ToggleGroup filters = new ToggleGroup();
  private final Label payload = Ui.label("Vælg en hændelse for at se dens payload", "muted");
  private final Notice notice = new Notice();

  public AdminOutboxView(Apis apis)
  {
    this.apis = apis;

    payload.getStyleClass().add("payload");
    payload.setMaxWidth(Double.MAX_VALUE);
    payload.setWrapText(true);

    TableView<Row> table = buildTable();

    root.getStyleClass().add("main");
    root.getChildren().addAll(buildFilters(), table,
                              Ui.kick("Payload"), payload,
                              Ui.spacer(), notice);

    VBox.setVgrow(table, Priority.ALWAYS);
  }

  @Override
  public String title()
  {
    return "Admin — Outbox";
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

  private Node buildFilters()
  {
    HBox segment = new HBox();

    for (Filter filter : Filter.values())
    {
      ToggleButton button = new ToggleButton(filter.label);
      button.setToggleGroup(filters);
      button.setUserData(filter);
      button.setOnAction(event ->
      {
        button.setSelected(true);
        applyFilter();
      });
      segment.getChildren().add(button);
    }

    ((ToggleButton) segment.getChildren().get(0)).setSelected(true);

    var refresh = Ui.ghost("Opdatér");
    refresh.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
    refresh.setOnAction(event -> reload());

    return Ui.row(12, segment, Ui.spacer(), refresh);
  }

  private TableView<Row> buildTable()
  {
    TableView<Row> table = new TableView<>(rows);
    table.setPlaceholder(Ui.label("Outboxen er tom", "muted"));
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

    TableColumn<Row, String> station = new TableColumn<>("STATION");
    station.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().station().displayName()));

    TableColumn<Row, String> type = new TableColumn<>("HÆNDELSE");
    type.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().entry().type()));

    TableColumn<Row, String> occurred = new TableColumn<>("TIDSPUNKT");
    occurred.setCellValueFactory(row ->
        new SimpleStringProperty(Ui.time(row.getValue().entry().occurredAt())));

    TableColumn<Row, String> status = new TableColumn<>("STATUS");
    status.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().entry().status()));
    status.setCellFactory(column -> badgeCell());

    table.getColumns().addAll(List.of(station, type, occurred, status));

    table.getSelectionModel().selectedItemProperty().addListener((observable, was, is) ->
        payload.setText(is == null ? "" : com.example.desktop.api.Json.pretty(is.entry().payload())));

    return table;
  }

  private TableCell<Row, String> badgeCell()
  {
    return new TableCell<>()
    {
      @Override
      protected void updateItem(String value, boolean empty)
      {
        super.updateItem(value, empty);

        if (empty || value == null)
        {
          setGraphic(null);
          return;
        }

        boolean failed = "FAILED".equals(value);

        Label badge = Ui.label(failed ? "Fejlet" : "I kø",
                               "badge", failed ? "badge-failed" : "badge-pending");

        setGraphic(badge);
        setText(null);
      }
    };
  }

  private void reload()
  {
    Async.run(this::collect,
              collected ->
              {
                loaded.clear();
                loaded.addAll(collected);
                applyFilter();
                notice.clear();
              },
              notice::showError);
  }

  private List<Row> collect()
  {
    List<Row> all = new ArrayList<>();

    for (Station station : Station.all())
    {
      for (OutboxEntryDTO entry : apis.station(station).outbox())
      {
        all.add(new Row(station, entry));
      }
    }

    all.sort(Comparator.comparing((Row row) -> row.entry().occurredAt()).reversed());

    return all;
  }

  private void applyFilter()
  {
    ToggleButton selected = (ToggleButton) filters.getSelectedToggle();
    Filter filter = selected == null ? Filter.ALL : (Filter) selected.getUserData();

    rows.setAll(loaded.stream().filter(row -> switch (filter)
    {
      case ALL -> true;
      case PENDING -> !row.entry().isFailed();
      case FAILED -> row.entry().isFailed();
    }).toList());
  }
}
