package view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import model.alert.Alert; 

public class GestionAlertsView extends BorderPane {

    public GestionAlertsView() {
        this.setPadding(new Insets(20));

        // Header: Title and action button
        Label title = new Label("Alert Management");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Button btnCreate = new Button("+ Create Alert");
        btnCreate.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        
        HBox header = new HBox(20, title, btnCreate);
        this.setTop(header);

        // Alert Table
        TableView<Alert> table = new TableView<>();
        
        // Colonne pour l'ID (attribut 'id' de ta classe Alert)
        TableColumn<Alert, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);
        
        // Colonne pour le Message (attribut 'message' de ta classe Alert)
        TableColumn<Alert, String> colMessage = new TableColumn<>("Message / Description");
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));
        colMessage.setPrefWidth(300);
        
        // Colonne pour l'Urgence (attribut 'urgencyLevel' de ta classe Alert)
        TableColumn<Alert, Integer> colUrgency = new TableColumn<>("Urgency Level");
        colUrgency.setCellValueFactory(new PropertyValueFactory<>("urgencyLevel"));
        colUrgency.setPrefWidth(120);

        // On ajoute les colonnes qui correspondent à ton modèle
        table.getColumns().addAll(colId, colMessage, colUrgency);
        
        // On remplit avec les fausses alertes adaptées à ton constructeur
        table.setItems(getAlertsMock());

        this.setCenter(table);
        BorderPane.setMargin(table, new Insets(20, 0, 0, 0));
    }

    /**
     * Génère une liste fictive d'alertes en respectant le constructeur :
     * new Alert(int id, String message, int urgencyLevel)
     */
    private ObservableList<Alert> getAlertsMock() {
        ObservableList<Alert> mockList = FXCollections.observableArrayList();
        
        // Utilise exactement la signature (int, String, int) de ton modèle !
        mockList.add(new Alert(1, "Crue subite détectée - Secteur Nord", 5));
        mockList.add(new Alert(2, "Pluies torrentielles - Évacuation préventive Zone B", 4));
        mockList.add(new Alert(3, "Vigilance météo orange activée", 2));
        
        return mockList;
    }
}