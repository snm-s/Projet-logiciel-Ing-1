package view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import model.alert.Alert;
import model.enums.AlertType;

/**
 * AdminAlertsView — Vue admin (plein pouvoir : ajouter / supprimer / résoudre / valider suggestions).
 * Reproduit EXACTEMENT la maquette : sidebar bleu marine + tableau blanc avec onglets.
 */
public class AdminAlertsView extends BorderPane {

    // ── couleurs exactes de la maquette ───────────────────────────────────
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
    private static final String BADGE_BG      = "#fef3c7";

    // ── state ─────────────────────────────────────────────────────────────
    private final ObservableList<Alert> allAlerts   = FXCollections.observableArrayList();
    private final ObservableList<Alert> suggestions = FXCollections.observableArrayList();
    private final TableView<Alert>      table       = new TableView<>();
    private String currentTab = "Toutes";
    private Label  tabToutes, tabActives, tabResolues, tabSuggestions;
    private controller.AdminAlertsController controller;

    public AdminAlertsView() {
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
            item.setOnMouseEntered(e -> item.setStyle(
                "-fx-background-color:" + SIDEBAR_HOVER + ";-fx-background-radius:8;"));
            item.setOnMouseExited(e -> item.setStyle(
                "-fx-background-color:transparent;-fx-background-radius:8;"));
        }
        return item;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONTENT PANEL
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(30, 30, 30, 30));
        content.setStyle("-fx-background-color:" + CONTENT_BG + ";");

        // ── Titre ──
        Label title = new Label("Gestion des alertes");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK + ";");

        // ── Carte principale ──
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
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);");
        card.setPadding(new Insets(20, 24, 0, 24));
        VBox.setVgrow(card, Priority.ALWAYS);

        // ── Ligne onglets + bouton ──
        HBox topRow = buildTopRow();

        // ── Séparateur sous les onglets ──
        Separator sep = new Separator();
        sep.setPadding(new Insets(0));

        // ── Tableau ──
        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        card.getChildren().addAll(topRow, sep, table);
        return card;
    }

    // ── Ligne : onglets à gauche, bouton à droite ──
    private HBox buildTopRow() {
        // Onglets
        tabToutes       = makeTab("Toutes",       true);
        tabActives      = makeTab("Actives",       false);
        tabResolues     = makeTab("Résolues",      false);
        tabSuggestions  = makeTab("Suggestions",   false);

        tabToutes.setOnMouseClicked(e      -> switchTab("Toutes"));
        tabActives.setOnMouseClicked(e     -> switchTab("Actives"));
        tabResolues.setOnMouseClicked(e    -> switchTab("Résolues"));
        tabSuggestions.setOnMouseClicked(e -> switchTab("Suggestions"));

        HBox tabs = new HBox(24, tabToutes, tabActives, tabResolues, tabSuggestions);
        tabs.setAlignment(Pos.CENTER_LEFT);

        // Bouton créer
        Button btnCreate = new Button("+ Créer une alerte");
        btnCreate.setStyle(
            "-fx-background-color:" + BLUE_BTN + ";" +
            "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;" +
            "-fx-padding:9 20;-fx-background-radius:8;-fx-cursor:hand;");
        btnCreate.setOnAction(e -> openCreateDialog());
        btnCreate.setOnMouseEntered(e -> btnCreate.setStyle(
            "-fx-background-color:#1d4ed8;-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-font-size:13px;-fx-padding:9 20;-fx-background-radius:8;-fx-cursor:hand;"));
        btnCreate.setOnMouseExited(e -> btnCreate.setStyle(
            "-fx-background-color:" + BLUE_BTN + ";-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-font-size:13px;-fx-padding:9 20;-fx-background-radius:8;-fx-cursor:hand;"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(tabs, spacer, btnCreate);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 0, 0));
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
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + BLUE_BTN + ";" +
                "-fx-border-color:transparent transparent " + BLUE_BTN + " transparent;" +
                "-fx-border-width:0 0 2.5 0;-fx-padding:12 4 12 4;");
        } else {
            lbl.setStyle(
                "-fx-font-size:13px;-fx-text-fill:" + TEXT_GRAY + ";-fx-padding:12 4 12 4;");
        }
    }

    private void switchTab(String tab) {
        currentTab = tab;
        styleTab(tabToutes,      "Toutes".equals(tab));
        styleTab(tabActives,     "Actives".equals(tab));
        styleTab(tabResolues,    "Résolues".equals(tab));
        styleTab(tabSuggestions, "Suggestions".equals(tab));
        refreshTable();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLE
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



        // ── Colonne Description ──
        TableColumn<Alert, String> colDesc = plainCol("Description", "description", 190);

        // ── Colonne Localisation ──
        TableColumn<Alert, String> colLoc = plainCol("Localisation", "localisation", 140);

        // ── Colonne Sévérité (colorée) ──
        TableColumn<Alert, String> colSev = new TableColumn<>("Sévérité");
        colSev.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colSev.setPrefWidth(90);
        colSev.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:"
                    + severityColor(v) + ";");
            }
        });

        // ── Colonne Heure ──
        TableColumn<Alert, String> colTime = plainCol("Heure", "time", 70);

        // ── Colonne Statut ──
        TableColumn<Alert, String> colStat = new TableColumn<>("Statut");
        colStat.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStat.setPrefWidth(90);
        colStat.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                String color = switch (v.toLowerCase()) {
                    case "active"      -> GREEN;
                    case "résolue"     -> TEXT_GRAY;
                    case "en attente"  -> YELLOW;
                    default            -> TEXT_DARK;
                };
                setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
            }
        });

        // ── Colonne Actions (admin seulement) ──
        TableColumn<Alert, Void> colAct = new TableColumn<>("Actions");
        colAct.setPrefWidth(160);
        colAct.setCellFactory(c -> new TableCell<>() {
            private final Button btnResolve  = actionBtn("Résoudre",  "#dcfce7", GREEN);
            private final Button btnDelete   = actionBtn("Supprimer", "#fee2e2", RED);
            private final Button btnApprove  = actionBtn("✓ Valider", "#dcfce7", GREEN);
            private final Button btnReject   = actionBtn("✕ Rejeter", "#fee2e2", RED);
            private final HBox   normalBox   = new HBox(6, btnResolve, btnDelete);
            private final HBox   suggBox     = new HBox(6, btnApprove, btnReject);
            {
                normalBox.setAlignment(Pos.CENTER_LEFT);
                suggBox.setAlignment(Pos.CENTER_LEFT);

                btnResolve.setOnAction(e -> {
                    Alert a = getTableView().getItems().get(getIndex());
                    if (controller != null) controller.resolveAlert(a);
                    else a.setStatus("Résolue");
                    refreshTable();
                });
                btnDelete.setOnAction(e -> {
                    Alert a = getTableView().getItems().get(getIndex());
                    if (controller != null) controller.deleteAlert(a);
                    else allAlerts.remove(a);
                    refreshTable();
                });
                btnApprove.setOnAction(e -> {
                    Alert a = getTableView().getItems().get(getIndex());
                    if (controller != null) controller.approveSuggestion(a);
                    else { suggestions.remove(a); a.setStatus("Active"); allAlerts.add(a); }
                    refreshTable();
                });
                btnReject.setOnAction(e -> {
                    Alert a = getTableView().getItems().get(getIndex());
                    if (controller != null) controller.rejectSuggestion(a);
                    else suggestions.remove(a);
                    refreshTable();
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Alert a = getTableView().getItems().get(getIndex());
                if (a.isSuggestion()) {
                    setGraphic(suggBox);
                } else {
                    btnResolve.setVisible(!"Résolue".equalsIgnoreCase(a.getStatus()));
                    setGraphic(normalBox);
                }
            }
        });

        table.getColumns().addAll(colType, colDesc, colLoc, colSev, colTime, colStat, colAct);

        // Alternance + hover des lignes
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
        Alert a = row.getItem();
        if (a != null && a.isSuggestion())
            row.setStyle("-fx-background-color:#fffbeb;");
        else
            row.setStyle(row.getIndex() % 2 == 0 ? "-fx-background-color:#ffffff;" : "-fx-background-color:#f8fafc;");
    }

    private Button actionBtn(String label, String bg, String fg) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
            "-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:5 10;" +
            "-fx-background-radius:5;-fx-cursor:hand;");
        return btn;
    }

    private <T> TableColumn<Alert, T> plainCol(String header, String prop, double w) {
        TableColumn<Alert, T> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setPrefWidth(w);
        
        col.setCellFactory(c -> new TableCell<Alert, T>() {
            @Override 
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // On utilise toString() pour afficher n'importe quel objet
                    setText(item.toString());
                    setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";");
                }
            }
        });
        return col;
    }

    // ── Filtrage ──────────────────────────────────────────────────────────
    public void refreshTable() {
        ObservableList<Alert> filtered = FXCollections.observableArrayList();
        switch (currentTab) {
            case "Actives"     -> allAlerts.stream().filter(a -> "Active".equalsIgnoreCase(a.getStatus())).forEach(filtered::add);
            case "Résolues"    -> allAlerts.stream().filter(a -> "Résolue".equalsIgnoreCase(a.getStatus())).forEach(filtered::add);
            case "Suggestions" -> filtered.addAll(suggestions);
            default            -> filtered.addAll(allAlerts);
        }
        table.setItems(filtered);
        table.refresh();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DIALOG CRÉATION
    // ══════════════════════════════════════════════════════════════════════
    private void openCreateDialog() {
        Dialog<Alert> dlg = new Dialog<>();
        dlg.setTitle("Créer une alerte");
        dlg.setHeaderText(null);

        ComboBox<AlertType> fType = comboEnum();   // ← menu déroulant enum
        TextField fDesc  = field("Description");
        TextField fLoc   = field("Localisation");
        ComboBox<String> fSev  = combo("Élevée","Moyenne","Faible");
        TextField fTime  = field("Heure  (HH:mm)");
        ComboBox<String> fStat = combo("Active","Résolue");
        fStat.setValue("Active");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 10, 24));
        grid.addRow(0, lbl("Type"),        fType);
        grid.addRow(1, lbl("Description"), fDesc);
        grid.addRow(2, lbl("Localisation"),fLoc);
        grid.addRow(3, lbl("Sévérité"),    fSev);
        grid.addRow(4, lbl("Heure"),       fTime);
        grid.addRow(5, lbl("Statut"),      fStat);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.setText("Créer");
        ok.setStyle("-fx-background-color:" + BLUE_BTN + ";-fx-text-fill:white;-fx-font-weight:bold;");

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            return new Alert(
                fType.getValue() == null   ? AlertType.INFO : fType.getValue(),  // ← AlertType direct
                fDesc.getText().isBlank()  ? "—"       : fDesc.getText().trim(),
                fLoc.getText().isBlank()   ? "—"       : fLoc.getText().trim(),
                fSev.getValue() == null    ? "Faible"  : fSev.getValue(),
                fTime.getText().isBlank()  ? "--:--"   : fTime.getText().trim(),
                fStat.getValue() == null   ? "Active"  : fStat.getValue(),
                "admin"
            );
        });

        dlg.showAndWait().ifPresent(a -> {
            if (controller != null) controller.addAlert(a);
            else allAlerts.add(a);
            refreshTable();
        });
    }

    private ComboBox<AlertType> comboEnum() {
        ComboBox<AlertType> cb = new ComboBox<>(
            FXCollections.observableArrayList(AlertType.values()));
        cb.setStyle("-fx-font-size:13px;-fx-pref-width:220px;");
        return cb;
    }

    // ── Helpers UI ────────────────────────────────────────────────────────
    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-font-size:13px;-fx-pref-width:220px;");
        return tf;
    }

    private ComboBox<String> combo(String... items) {
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(items));
        cb.setStyle("-fx-font-size:13px;-fx-pref-width:220px;");
        return cb;
    }

    private Label lbl(String text) {
        Label l = new Label(text + " :");
        l.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";-fx-min-width:90px;");
        return l;
    }

    // ── Helpers métier ────────────────────────────────────────────────────
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

    private void seedData() {
        allAlerts.addAll(
            new Alert(AlertType.INONDATION,    "Route D12 inondée",         "D12 - Pont Nord", "Élevée",  "10:24", "Active"),
            new Alert(AlertType.ROUTE, "Pont des Lilas fermé",      "Pont des Lilas",  "Élevée",  "09:58", "Active"),
            new Alert(AlertType.EVACUATION,    "Quartier Gare à évacuer",   "Quartier Gare",   "Moyenne", "09:12", "Active"),
            new Alert(AlertType.INFO,          "Hôpital Central accessible","Hôpital Central", "Faible",  "08:45", "Active"),
            new Alert(AlertType.INFO,          "Niveau d'eau en baisse",    "Rivière Seine",   "Faible",  "07:30", "Résolue")
        );
        // Exemple de suggestion en attente
        Alert s = new Alert(AlertType.INONDATION, "Sous-sol rue Carnot inondé", "Rue Carnot 12", "Moyenne", "11:05", "En attente", "suggestion");
        suggestions.add(s);
    }

    // ── Public API ────────────────────────────────────────────────────────
    public ObservableList<Alert> getAllAlerts()    { return allAlerts; }
    public ObservableList<Alert> getSuggestions()  { return suggestions; }
    public TableView<Alert>      getTable()        { return table; }
    public void setController(controller.AdminAlertsController c) { this.controller = c; }
}
