package view;

import controller.RescuePage.RescueController;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.alert.Alert;

public class RescueAlertsView extends BorderPane {
    /**
     * Constructs a new RescueAlertsView.
     * @param controller the controller.
     */
    public RescueAlertsView(RescueController controller) {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));

        VBox root = new VBox(16);
        root.getChildren().addAll(label("Alertes", "#ffffff", 24, true), label("Alertes actives et informations terrain.", "#b8c7dd", 13, false));

        TableView<Alert> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-border-color:rgba(255,255,255,0.18);");

        TableColumn<Alert, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Alert, String> desc = new TableColumn<>("Description");
        desc.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Alert, String> loc = new TableColumn<>("Localisation");
        loc.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        TableColumn<Alert, String> sev = new TableColumn<>("Sévérité");
        sev.setCellValueFactory(new PropertyValueFactory<>("severity"));
        TableColumn<Alert, String> time = new TableColumn<>("Heure");
        time.setCellValueFactory(new PropertyValueFactory<>("time"));
        TableColumn<Alert, String> status = new TableColumn<>("Statut");
        status.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(type, desc, loc, sev, time, status);
        table.setItems(FXCollections.observableArrayList(controller.getRecentAlerts()));

        root.getChildren().add(table);
        setCenter(root);
    }

    /**
     * Performs label.
     * @param text the text.
     * @param color the color.
     * @param size the size.
     * @param bold the bold.
     * @return the Label.
     */
    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }
}
