package view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import model.alert.Alert;
import model.enums.AlertType;

/**
 * CitizenAlertsView — Vue citoyen : consultation des alertes + formulaire de suggestion.
 * Même charte graphique que AdminAlertsView. Pas de bouton créer/supprimer dans le tableau.
 */
public class CitizenAlertsView extends BorderPane {

    private static final String SIDEBAR_BG    = "#1e3a5f";
    private static final String SIDEBAR_HOVER = "#2a4f82";
    private static final String SIDEBAR_ACTIVE= "#2563eb";
    private static final String CONTENT_BG    = "#e8edf5";
    private static final String CARD_BG       = "#ffffff";
    private static final String BLUE_BTN      = "#2563eb";
    private static final String BORDER        = "#e5e9f0";
    private static final String TEXT_DARK     = "#1a2744";
    private static final String TEXT_GRAY     = "#9aa5b4";
    private static final String RED           = "#dc2626";
    private static final String GREEN         = "#16a34a";
    private static final String ORANGE        = "#ea580c";
    private static final String YELLOW        = "#ca8a04";

    private final ObservableList<Alert> allAlerts = FXCollections.observableArrayList();
    private final TableView<Alert>      table     = new TableView<>();
    private String currentTab = "Toutes";
    private Label  tabToutes, tabActives, tabResolues;
    private controller.CitizenAlertsController controller;

    public CitizenAlertsView() {
        this.setStyle("-fx-background-color:" + CONTENT_BG + ";");
        seedData();
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
            item.setOnMouseEntered(e -> item.setStyle("-fx-background-color:" + SIDEBAR_HOVER + ";-fx-background-radius:8;"));
            item.setOnMouseExited(e  -> item.setStyle("-fx-background-color:transparent;-fx-background-radius:8;"));
        }
        return item;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONTENT
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30, 30, 30, 30));
        content.setStyle("-fx-background-color:" + CONTENT_BG + ";");

        Label title = new Label("Alertes en cours");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK + ";");

        // Bandeau info citoyen
        HBox infoBanner = new HBox(10);
        infoBanner.setStyle(
            "-fx-background-color:#eff6ff;-fx-background-radius:8;" +
            "-fx-border-color:#bfdbfe;-fx-border-radius:8;");
        infoBanner.setPadding(new Insets(12, 16, 12, 16));
        infoBanner.setAlignment(Pos.CENTER_LEFT);
        Label infoIco = new Label("ℹ");
        infoIco.setStyle("-fx-text-fill:" + BLUE_BTN + ";-fx-font-size:14px;");
        Label infoTxt = new Label("Vous consultez les alertes publiées. Pour signaler une situation, utilisez le bouton « Suggérer une alerte ».");
        infoTxt.setStyle("-fx-text-fill:#1e40af;-fx-font-size:13px;");
        infoTxt.setWrapText(true);
        infoBanner.getChildren().addAll(infoIco, infoTxt);

        VBox card = buildCard();
        VBox.setVgrow(card, Priority.ALWAYS);

        content.getChildren().addAll(title, infoBanner, card);
        VBox.setVgrow(content, Priority.ALWAYS);
        return content;
    }

    private VBox buildCard() {
        VBox card = new VBox(0);
        card.setStyle(
            "-fx-background-color:" + CARD_BG + ";" +
            "-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);");
        card.setPadding(new Insets(20, 24, 0, 24));
        VBox.setVgrow(card, Priority.ALWAYS);

        // ── Ligne onglets + bouton suggérer ──
        tabToutes   = makeTab("Toutes",  true);
        tabActives  = makeTab("Actives", false);
        tabResolues = makeTab("Résolues",false);

        tabToutes.setOnMouseClicked(e   -> switchTab("Toutes"));
        tabActives.setOnMouseClicked(e  -> switchTab("Actives"));
        tabResolues.setOnMouseClicked(e -> switchTab("Résolues"));

        HBox tabs = new HBox(24, tabToutes, tabActives, tabResolues);
        tabs.setAlignment(Pos.CENTER_LEFT);

        // Bouton suggérer (citoyen ne peut pas créer directement)
        Button btnSuggest = new Button("💡 Suggérer une alerte");
        btnSuggest.setStyle(
            "-fx-background-color:#f0f9ff;-fx-text-fill:" + BLUE_BTN + ";" +
            "-fx-font-weight:bold;-fx-font-size:13px;" +
            "-fx-padding:9 20;-fx-background-radius:8;-fx-cursor:hand;" +
            "-fx-border-color:" + BLUE_BTN + ";-fx-border-radius:8;-fx-border-width:1.5;");
        btnSuggest.setOnAction(e -> openSuggestDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(tabs, spacer, btnSuggest);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        card.getChildren().addAll(topRow, sep, table);
        return card;
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
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + BLUE_BTN + ";" +
                "-fx-border-color:transparent transparent " + BLUE_BTN + " transparent;" +
                "-fx-border-width:0 0 2.5 0;-fx-padding:12 4 12 4;");
        } else {
            lbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_GRAY + ";-fx-padding:12 4 12 4;");
        }
    }

    private void switchTab(String tab) {
        currentTab = tab;
        styleTab(tabToutes,   "Toutes".equals(tab));
        styleTab(tabActives,  "Actives".equals(tab));
        styleTab(tabResolues, "Résolues".equals(tab));
        refreshTable();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLE (lecture seule — pas de bouton supprimer/résoudre)
    // ══════════════════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        table.setFixedCellSize(52);

        TableColumn<Alert, AlertType> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getType()));
        colType.setPrefWidth(140);
        colType.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(AlertType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                String v = item.toString();
                HBox box = new HBox(8); box.setAlignment(Pos.CENTER_LEFT);
                Label ico = new Label(typeIcon(v)); ico.setStyle("-fx-font-size:16px;");
                Label txt = new Label(v); txt.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";");
                box.getChildren().addAll(ico, txt);
                setGraphic(box); setText(null);
                setStyle("-fx-padding:0 0 0 4;");
            }
        });

        TableColumn<Alert, String> colDesc = plainCol("Description",  "description",  190);
        TableColumn<Alert, String> colLoc  = plainCol("Localisation",  "localisation", 140);

        TableColumn<Alert, String> colSev = new TableColumn<>("Sévérité");
        colSev.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colSev.setPrefWidth(90);
        colSev.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + severityColor(v) + ";");
            }
        });

        TableColumn<Alert, String> colTime = plainCol("Heure", "time", 70);

        TableColumn<Alert, String> colStat = new TableColumn<>("Statut");
        colStat.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStat.setPrefWidth(90);
        colStat.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                String color = "Active".equalsIgnoreCase(v) ? GREEN : TEXT_GRAY;
                setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
            }
        });

        table.getColumns().addAll(colType, colDesc, colLoc, colSev, colTime, colStat);

        table.setRowFactory(tv -> {
            TableRow<Alert> row = new TableRow<>();
            row.indexProperty().addListener((obs, o, n) -> applyRowStyle(row));
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle("-fx-background-color:#f0f5ff;"); });
            row.setOnMouseExited(e  -> applyRowStyle(row));
            return row;
        });

        refreshTable();
    }

    private void applyRowStyle(TableRow<Alert> row) {
        if (row.isEmpty()) return;
        row.setStyle(row.getIndex() % 2 == 0 ? "-fx-background-color:#ffffff;" : "-fx-background-color:#f8fafc;");
    }

    private TableColumn<Alert, String> plainCol(String header, String prop, double w) {
        TableColumn<Alert, String> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setPrefWidth(w);
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";");
            }
        });
        return col;
    }

    public void refreshTable() {
        ObservableList<Alert> filtered = FXCollections.observableArrayList();
        switch (currentTab) {
            case "Actives"  -> allAlerts.stream().filter(a -> "Active".equalsIgnoreCase(a.getStatus())).forEach(filtered::add);
            case "Résolues" -> allAlerts.stream().filter(a -> "Résolue".equalsIgnoreCase(a.getStatus())).forEach(filtered::add);
            default         -> filtered.addAll(allAlerts);
        }
        table.setItems(filtered);
        table.refresh();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DIALOG SUGGESTION (citoyen)
    // ══════════════════════════════════════════════════════════════════════
    private void openSuggestDialog() {
        Dialog<Alert> dlg = new Dialog<>();
        dlg.setTitle("Suggérer une alerte");
        dlg.setHeaderText("Votre suggestion sera envoyée à l'administrateur pour validation.");

        ComboBox<AlertType> fType = comboEnum();   // ← menu déroulant enum
        TextField fDesc = field("Description de la situation");
        TextField fLoc  = field("Adresse / Localisation");
        ComboBox<String> fSev = combo("Élevée","Moyenne","Faible");
        TextField fTime = field("Heure constatée (HH:mm)");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(16, 24, 10, 24));
        grid.addRow(0, lbl("Type"),        fType);
        grid.addRow(1, lbl("Description"), fDesc);
        grid.addRow(2, lbl("Localisation"),fLoc);
        grid.addRow(3, lbl("Sévérité"),    fSev);
        grid.addRow(4, lbl("Heure"),       fTime);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Envoyer la suggestion");
        ok.setStyle("-fx-background-color:" + BLUE_BTN + ";-fx-text-fill:white;-fx-font-weight:bold;");

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            return new Alert(
                fType.getValue() == null  ? AlertType.INFO : fType.getValue(),  // ← AlertType direct
                fDesc.getText().isBlank() ? "—"      : fDesc.getText().trim(),
                fLoc.getText().isBlank()  ? "—"      : fLoc.getText().trim(),
                fSev.getValue() == null   ? "Faible" : fSev.getValue(),
                fTime.getText().isBlank() ? "--:--"  : fTime.getText().trim(),
                "En attente",
                "suggestion"
            );
        });

        dlg.showAndWait().ifPresent(a -> {
            if (controller != null) {
                controller.submitSuggestion(a);
                showConfirmation();
            }
        });
    }


    private ComboBox<AlertType> comboEnum() {
        ComboBox<AlertType> cb = new ComboBox<>(
            FXCollections.observableArrayList(AlertType.values()));
        cb.setStyle("-fx-font-size:13px;-fx-pref-width:240px;");
        return cb;
    }


    private void showConfirmation() {
        // model.alert.Alert masque javafx.scene.control.Alert via l'import
        // → on utilise Dialog<Void> pour éviter le conflit de nom.
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Suggestion envoyée");
        dlg.setHeaderText(null);

        Label msg = new Label("✅ Votre suggestion a bien été transmise à\nl'administrateur. Elle sera visible après validation.");
        msg.setStyle("-fx-font-size:13px;-fx-text-fill:#1a2744;-fx-padding:10 16 10 16;");
        msg.setWrapText(true);
        dlg.getDialogPane().setContent(msg);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color:#2563eb;-fx-text-fill:white;-fx-font-weight:bold;");

        dlg.showAndWait();
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private String typeIcon(String type) {
        if (type == null) return "ℹ";
        return switch (type.toUpperCase()) {   // ← toUpperCase pour matcher l'enum
            case "INONDATION" -> "🌊";
            case "ROUTE"      -> "🚧";
            case "EVACUATION" -> "⚠";
            case "AUTRE"      -> "⚡";
            default           -> "ℹ";         // INFO et cas inconnus
        };
    }

    private String severityColor(String s) {
        if (s == null) return TEXT_GRAY;
        return switch (s.toLowerCase()) {
            case "élevée"  -> RED;
            case "moyenne" -> ORANGE;
            default        -> TEXT_GRAY;
        };
    }

    private TextField field(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt);
        tf.setStyle("-fx-font-size:13px;-fx-pref-width:240px;");
        return tf;
    }

    private ComboBox<String> combo(String... items) {
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(items));
        cb.setStyle("-fx-font-size:13px;-fx-pref-width:240px;");
        return cb;
    }

    private Label lbl(String text) {
        Label l = new Label(text + " :");
        l.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";-fx-min-width:90px;");
        return l;
    }

    private void seedData() {
        allAlerts.addAll(
            new Alert(AlertType.INONDATION,    "Route D12 inondée",         "D12 - Pont Nord", "Élevée",  "10:24", "Active"),
            new Alert(AlertType.ROUTE, "Pont des Lilas fermé",      "Pont des Lilas",  "Élevée",  "09:58", "Active"),
            new Alert(AlertType.EVACUATION,    "Quartier Gare à évacuer",   "Quartier Gare",   "Moyenne", "09:12", "Active"),
            new Alert(AlertType.INFO,          "Hôpital Central accessible","Hôpital Central", "Faible",  "08:45", "Active"),
            new Alert(AlertType.INFO,          "Niveau d'eau en baisse",    "Rivière Seine",   "Faible",  "07:30", "Résolue")
        );
    }

    // ── Public API ────────────────────────────────────────────────────────
    public ObservableList<Alert> getAllAlerts() { return allAlerts; }
    public TableView<Alert>      getTable()     { return table; }
    public void setController(controller.CitizenAlertsController c) { this.controller = c; }
}