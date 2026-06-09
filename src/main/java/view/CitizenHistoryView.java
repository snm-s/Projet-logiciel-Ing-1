package view;

import controller.CitizenPage.CitizenController;
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

public class CitizenHistoryView extends BorderPane {
    public CitizenHistoryView(CitizenController controller) {
        setStyle("-fx-background-color:transparent;");
        setPadding(new Insets(26));
        VBox root = new VBox(16);
        Label title = label("Historique", "#ffffff", 24, true);
        Label sub = label("Historique de vos alertes, déplacements et consignes reçues.", "#b8c7dd", 13, false);

        TableView<HistoryItem> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color:rgba(8,22,42,0.72); -fx-border-color:rgba(255,255,255,0.18); -fx-text-fill:white;");

        TableColumn<HistoryItem, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<HistoryItem, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<HistoryItem, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<HistoryItem, String> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colDate, colType, colDesc, colStatus);
        table.setItems(FXCollections.observableArrayList(
                new HistoryItem("Aujourd'hui 10:24", "Alerte", "Route inondée", "Active"),
                new HistoryItem("Aujourd'hui 09:12", "Évacuation", "Refuge conseillé", "Traitée"),
                new HistoryItem("Hier 18:40", "Trajet", "Itinéraire recalculé", "Terminé")
        ));

        root.getChildren().addAll(title, sub, table);
        setCenter(root);
    }

    private Label label(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setTextFill(Color.web(color));
        l.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return l;
    }

    public static class HistoryItem {
        private final String date, type, description, status;
        public HistoryItem(String date, String type, String description, String status) {
            this.date = date; this.type = type; this.description = description; this.status = status;
        }
        public String getDate() { return date; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
    }
}
