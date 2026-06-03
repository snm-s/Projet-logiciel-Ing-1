import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class AlertsView extends BorderPane {

    public AlertsView() {
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
        
        TableColumn<Alert, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        TableColumn<Alert, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        TableColumn<Alert, String> colLocation = new TableColumn<>("Location");
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        
        TableColumn<Alert, String> colSeverity = new TableColumn<>("Severity");
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        
        TableColumn<Alert, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colType, colDesc, colLocation, colSeverity, colStatus);
        table.setItems(getAlertsMock());

        this.setCenter(table);
        BorderPane.setMargin(table, new Insets(20, 0, 0, 0));
    }


}