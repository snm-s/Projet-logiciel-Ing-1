package view;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.alert.Alert;
import model.enums.AlertSeverity;
import model.enums.AlertStatus;
import model.enums.AlertType;

public class AdminAlertsView extends BorderPane {

    private static final String SIDEBAR_BG = "#1e3a5f";
    private static final String SIDEBAR_HOVER = "#2a4f82";
    private static final String SIDEBAR_ACTIVE = "#2563eb";
    private static final String CONTENT_BG = "#e8edf5";
    private static final String CARD_BG = "#ffffff";
    private static final String BLUE_BTN = "#2563eb";
    private static final String TEXT_DARK = "#1a2744";
    private static final String TEXT_GRAY = "#9aa5b4";
    private static final String RED = "#dc2626";
    private static final String GREEN = "#16a34a";
    private static final String ORANGE = "#ea580c";
    private static final String YELLOW = "#ca8a04";

    private final ObservableList<Alert> allAlerts = FXCollections.observableArrayList();
    private final ObservableList<Alert> suggestions = FXCollections.observableArrayList();
    private final TableView<Alert> table = new TableView<>();

    private String currentTab = "Toutes";

    private Label tabToutes;
    private Label tabActives;
    private Label tabResolues;
    private Label tabSuggestions;

    private controller.AdminPage.AdminAlertsController controller;

    public AdminAlertsView() {
        this.setStyle("-fx-background-color:" + CONTENT_BG + ";");
        this.setCenter(buildContent());
    }

    private HBox sideItem(String icon, String label, boolean active) {
        HBox item = new HBox(12);
        item.setPadding(new Insets(10, 14, 10, 14));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setCursor(Cursor.HAND);
        item.setStyle(active
                ? "-fx-background-color:" + SIDEBAR_ACTIVE + ";-fx-background-radius:8;"
                : "-fx-background-color:transparent;-fx-background-radius:8;");

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size:15px;-fx-text-fill:" + (active ? "white" : "#7fa0cc") + ";");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + (active ? "white" : "#9fbbd4") + ";"
                + (active ? "-fx-font-weight:bold;" : ""));

        item.getChildren().addAll(ico, lbl);

        if (!active) {
            item.setOnMouseEntered(e -> item.setStyle(
                    "-fx-background-color:" + SIDEBAR_HOVER + ";-fx-background-radius:8;"));
            item.setOnMouseExited(e -> item.setStyle(
                    "-fx-background-color:transparent;-fx-background-radius:8;"));
        }

        return item;
    }

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30, 30, 30, 30));
        content.setStyle("-fx-background-color:" + CONTENT_BG + ";");

        Label title = new Label("Gestion des alertes");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK + ";");

        VBox card = buildCard();
        VBox.setVgrow(card, Priority.ALWAYS);

        content.getChildren().addAll(title, card);
        VBox.setVgrow(content, Priority.ALWAYS);

        return content;
    }

    private VBox buildCard() {
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color:" + CARD_BG + ";" +
                        "-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);"
        );
        card.setPadding(new Insets(20, 24, 0, 24));
        VBox.setVgrow(card, Priority.ALWAYS);

        HBox topRow = buildTopRow();

        Separator sep = new Separator();
        sep.setPadding(new Insets(0));

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        card.getChildren().addAll(topRow, sep, table);

        return card;
    }

    private HBox buildTopRow() {
        tabToutes = makeTab("Toutes", true);
        tabActives = makeTab("Actives", false);
        tabResolues = makeTab("Résolues", false);
        tabSuggestions = makeTab("Suggestions", false);

        tabToutes.setOnMouseClicked(e -> switchTab("Toutes"));
        tabActives.setOnMouseClicked(e -> switchTab("Actives"));
        tabResolues.setOnMouseClicked(e -> switchTab("Résolues"));
        tabSuggestions.setOnMouseClicked(e -> switchTab("Suggestions"));

        HBox tabs = new HBox(24, tabToutes, tabActives, tabResolues, tabSuggestions);
        tabs.setAlignment(Pos.CENTER_LEFT);

        Button btnCreate = new Button("+ Créer une alerte");
        btnCreate.setStyle(
                "-fx-background-color:" + BLUE_BTN + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:13px;" +
                        "-fx-padding:9 20;" +
                        "-fx-background-radius:8;" +
                        "-fx-cursor:hand;"
        );

        btnCreate.setOnAction(e -> openCreateDialog());

        btnCreate.setOnMouseEntered(e -> btnCreate.setStyle(
                "-fx-background-color:#1d4ed8;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:13px;" +
                        "-fx-padding:9 20;" +
                        "-fx-background-radius:8;" +
                        "-fx-cursor:hand;"
        ));

        btnCreate.setOnMouseExited(e -> btnCreate.setStyle(
                "-fx-background-color:" + BLUE_BTN + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:13px;" +
                        "-fx-padding:9 20;" +
                        "-fx-background-radius:8;" +
                        "-fx-cursor:hand;"
        ));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(tabs, spacer, btnCreate);
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    private Label makeTab(String text, boolean active) {
        Label lbl = new Label(text);
        lbl.setCursor(Cursor.HAND);
        styleTab(lbl, active);
        return lbl;
    }

    private void styleTab(Label lbl, boolean active) {
        if (active) {
            lbl.setStyle(
                    "-fx-font-size:13px;" +
                            "-fx-font-weight:bold;" +
                            "-fx-text-fill:" + BLUE_BTN + ";" +
                            "-fx-border-color:transparent transparent " + BLUE_BTN + " transparent;" +
                            "-fx-border-width:0 0 2.5 0;" +
                            "-fx-padding:12 4 12 4;"
            );
        } else {
            lbl.setStyle(
                    "-fx-font-size:13px;" +
                            "-fx-text-fill:" + TEXT_GRAY + ";" +
                            "-fx-padding:12 4 12 4;"
            );
        }
    }

    private void switchTab(String tab) {
        currentTab = tab;

        styleTab(tabToutes, "Toutes".equals(tab));
        styleTab(tabActives, "Actives".equals(tab));
        styleTab(tabResolues, "Résolues".equals(tab));
        styleTab(tabSuggestions, "Suggestions".equals(tab));

        refreshTable();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        table.setFixedCellSize(52);

        TableColumn<Alert, AlertType> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getType()));
        colType.setPrefWidth(140);
        colType.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(AlertType item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);

                Label code = new Label(typeCode(item));
                code.setStyle(
                        "-fx-font-size:11px;" +
                                "-fx-font-weight:bold;" +
                                "-fx-text-fill:" + BLUE_BTN + ";" +
                                "-fx-background-color:#eff6ff;" +
                                "-fx-background-radius:6;" +
                                "-fx-padding:4 7;"
                );

                Label txt = new Label(formatType(item));
                txt.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";");

                box.getChildren().addAll(code, txt);

                setGraphic(box);
                setText(null);
                setStyle("-fx-padding:0 0 0 4;");
            }
        });

        TableColumn<Alert, String> colDesc = plainCol("Description", "description", 190);
        TableColumn<Alert, String> colLoc = plainCol("Localisation", "localisation", 140);

        TableColumn<Alert, AlertSeverity> colSev = new TableColumn<>("Sévérité");
        colSev.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colSev.setPrefWidth(90);
        colSev.setCellFactory(c -> new TableCell<Alert, AlertSeverity>() {

            @Override
            protected void updateItem(AlertSeverity value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(value.toString());

                setStyle(
                        "-fx-font-size:13px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + severityColor(value) + ";"
                );
            }
        });

        TableColumn<Alert, String> colTime = plainCol("Heure", "time", 70);

        TableColumn<Alert, String> colStat = new TableColumn<>("Statut");
        colStat.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStat.setPrefWidth(90);
        colStat.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(value);

                String color = switch (value.toLowerCase()) {
                    case "active" -> GREEN;
                    case "résolue" -> TEXT_GRAY;
                    case "en attente" -> YELLOW;
                    default -> TEXT_DARK;
                };

                setStyle(
                        "-fx-font-size:13px;" +
                                "-fx-font-weight:bold;" +
                                "-fx-text-fill:" + color + ";"
                );
            }
        });

        TableColumn<Alert, Void> colAct = new TableColumn<>("Actions");
        colAct.setPrefWidth(170);
        colAct.setCellFactory(c -> new TableCell<>() {
            private final Button btnResolve = actionBtn("Résoudre", "#dcfce7", GREEN);
            private final Button btnDelete = actionBtn("Supprimer", "#fee2e2", RED);
            private final Button btnApprove = actionBtn("Valider", "#dcfce7", GREEN);
            private final Button btnReject = actionBtn("Rejeter", "#fee2e2", RED);

            private final HBox normalBox = new HBox(6, btnResolve, btnDelete);
            private final HBox suggBox = new HBox(6, btnApprove, btnReject);

            {
                normalBox.setAlignment(Pos.CENTER_LEFT);
                suggBox.setAlignment(Pos.CENTER_LEFT);

                btnResolve.setOnAction(e -> {
                    Alert alert = getTableView().getItems().get(getIndex());

                    if (controller != null) {
                        controller.resolveAlert(alert);
                    } else {
                        alert.setStatus("Résolue");
                    }

                    refreshTable();
                });

                btnDelete.setOnAction(e -> {
                    Alert alert = getTableView().getItems().get(getIndex());

                    if (controller != null) {
                        controller.deleteAlert(alert);
                    } else {
                        allAlerts.remove(alert);
                    }

                    refreshTable();
                });

                btnApprove.setOnAction(e -> {
                    Alert alert = getTableView().getItems().get(getIndex());

                    if (controller != null) {
                        controller.approveSuggestion(alert);
                    } else {
                        suggestions.remove(alert);
                        alert.setStatus("Active");
                        alert.setOrigin("admin");
                        allAlerts.add(alert);
                    }

                    refreshTable();
                });

                btnReject.setOnAction(e -> {
                    Alert alert = getTableView().getItems().get(getIndex());

                    if (controller != null) {
                        controller.rejectSuggestion(alert);
                    } else {
                        suggestions.remove(alert);
                    }

                    refreshTable();
                });
            }

            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Alert alert = getTableView().getItems().get(getIndex());

                if (alert.isSuggestion()) {
                    setGraphic(suggBox);
                } else {
                    btnResolve.setVisible(!"Résolue".equalsIgnoreCase(alert.getStatus()));
                    setGraphic(normalBox);
                }
            }
        });

        table.getColumns().addAll(colType, colDesc, colLoc, colSev, colTime, colStat, colAct);

        table.setRowFactory(tv -> {
            TableRow<Alert> row = new TableRow<>();

            row.indexProperty().addListener((obs, oldValue, newValue) -> applyRowStyle(row));

            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-background-color:#f0f5ff;");
                }
            });

            row.setOnMouseExited(e -> applyRowStyle(row));

            return row;
        });

        refreshTable();
    }

    private void applyRowStyle(TableRow<Alert> row) {
        if (row.isEmpty()) return;

        Alert alert = row.getItem();

        if (alert != null && alert.isSuggestion()) {
            row.setStyle("-fx-background-color:#fffbeb;");
        } else {
            row.setStyle(row.getIndex() % 2 == 0 ? "-fx-background-color:#ffffff;" : "-fx-background-color:#f8fafc;");
        }
    }

    private Button actionBtn(String label, String bg, String fg) {
        Button btn = new Button(label);
        btn.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-size:11px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-padding:5 10;" +
                        "-fx-background-radius:5;" +
                        "-fx-cursor:hand;"
        );

        return btn;
    }

    private <T> TableColumn<Alert, T> plainCol(String header, String prop, double width) {
        TableColumn<Alert, T> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setPrefWidth(width);

        col.setCellFactory(c -> new TableCell<Alert, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";");
                }
            }
        });

        return col;
    }

    public void refreshTable() {
        ObservableList<Alert> filtered = FXCollections.observableArrayList();

        switch (currentTab) {
            case "Actives" -> allAlerts.stream()
                    .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                    .forEach(filtered::add);

            case "Résolues" -> allAlerts.stream()
                    .filter(a -> "Résolue".equalsIgnoreCase(a.getStatus()))
                    .forEach(filtered::add);

            case "Suggestions" -> filtered.addAll(suggestions);

            default -> filtered.addAll(allAlerts);
        }

        table.setItems(filtered);
        table.refresh();
    }

    private void openCreateDialog() {
        Dialog<Alert> dlg = new Dialog<>();
        dlg.setTitle("Créer une alerte");
        dlg.setHeaderText(null);

        ComboBox<AlertType> fType = comboEnum();
        TextField fDesc = field("Description");
        TextField fLoc = field("Localisation");
        ComboBox<AlertSeverity> fSev = comboSeverity();
        TextField fTime = field("Heure (HH:mm)");
        ComboBox<String> fStat = combo("Active", "Résolue");

        fStat.setValue("Active");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 10, 24));

        grid.addRow(0, lbl("Type"), fType);
        grid.addRow(1, lbl("Description"), fDesc);
        grid.addRow(2, lbl("Localisation"), fLoc);
        grid.addRow(3, lbl("Sévérité"), fSev);
        grid.addRow(4, lbl("Heure"), fTime);
        grid.addRow(5, lbl("Statut"), fStat);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Créer");
        ok.setStyle("-fx-background-color:" + BLUE_BTN + ";-fx-text-fill:white;-fx-font-weight:bold;");

        dlg.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;


            return new Alert(
                    fType.getValue() == null ? AlertType.INFO : fType.getValue(),
                    fDesc.getText().isBlank() ? "—" : fDesc.getText().trim(),
                    fLoc.getText().isBlank() ? "—" : fLoc.getText().trim(),
                    fSev.getValue() == null ? AlertSeverity.LOW : fSev.getValue(),
                    fTime.getText().isBlank() ? "--:--" : fTime.getText().trim(),
                    fStat.getValue() == null ? "Active" : fStat.getValue(),
                    "admin"
            );
        });

        dlg.showAndWait().ifPresent(alert -> {
            if (controller != null) {
                controller.addAlert(alert);
            } else {
                allAlerts.add(alert);
                refreshTable();
            }
        });
    }

    private ComboBox<AlertType> comboEnum() {
        ComboBox<AlertType> cb = new ComboBox<>(FXCollections.observableArrayList(AlertType.values()));
        cb.setStyle("-fx-font-size:13px;-fx-pref-width:220px;");
        return cb;
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-font-size:13px;-fx-pref-width:220px;");
        return tf;
    }

    private ComboBox<AlertSeverity> comboSeverity() {
        ComboBox<AlertSeverity> cb = new ComboBox<>(FXCollections.observableArrayList(AlertSeverity.values()));
        cb.setStyle("-fx-font-size:13px;-fx-pref-width:220px;");
        return cb;
    }

    private ComboBox<String> combo(String... items) {
        ComboBox<String> cb = new ComboBox<>(
                FXCollections.observableArrayList(items)
        );
        cb.setStyle("-fx-font-size:13px;-fx-pref-width:220px;");
        return cb;
    }

    private Label lbl(String text) {
        Label label = new Label(text + " :");
        label.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";-fx-min-width:90px;");
        return label;
    }

    private String typeCode(AlertType type) {
        if (type == null) return "IF";

        return switch (type.toString().toUpperCase()) {
            case "INONDATION" -> "IN";
            case "ROUTE" -> "RT";
            case "EVACUATION" -> "EV";
            case "AUTRE" -> "AU";
            default -> "IF";
        };
    }

    private String formatType(AlertType type) {
        if (type == null) return "Information";

        return switch (type.toString().toUpperCase()) {
            case "INONDATION" -> "Inondation";
            case "ROUTE" -> "Route";
            case "EVACUATION" -> "Évacuation";
            case "AUTRE" -> "Autre";
            default -> "Information";
        };
    }

    private String severityColor(AlertSeverity severity) {
        if (severity == null) return TEXT_GRAY;

        return switch (severity) {
            case HIGH -> RED;
            case WARNING -> ORANGE;
            default -> TEXT_GRAY;
        };
    }

    public void setAlerts(List<Alert> alerts) {
        allAlerts.setAll(alerts);
        refreshTable();
    }

    public void setSuggestions(List<Alert> newSuggestions) {
        suggestions.setAll(newSuggestions);
        refreshTable();
    }

    public ObservableList<Alert> getAllAlerts() {
        return allAlerts;
    }

    public ObservableList<Alert> getSuggestions() {
        return suggestions;
    }

    public TableView<Alert> getTable() {
        return table;
    }

    public void setController(controller.AdminPage.AdminAlertsController controller) {
        this.controller = controller;
    }
}