package model.alert;

import model.enums.AlertSeverity;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import model.enums.AlertStatus;
import model.enums.AlertType;
import model.graph.Node;

public class Alert {
    private int id;
    private AlertType type;
    private String description;

    private Node localisation;
    private AlertSeverity severity;
    private String time;
    private String status; // "Active", "Résolue", "En attente"
    private String origin; // "admin" | "suggestion"

    public Alert(AlertType type, String description, Node localisation,
            AlertSeverity severity, String time, String status) {
        this(type, description, localisation, severity, time, status, "admin");
    }

    public Alert(AlertType type, String description, Node localisation,
            AlertSeverity severity, String time, String status, String origin) {
        this.id = 0;
        this.type = type;
        this.description = description;
        this.localisation = localisation;
        this.severity = severity;
        this.time = time;
        this.status = status;
        this.origin = origin;
    }

    // Getters
    public int getId() {
        return id;
    }

    public AlertType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Node getLocalisation() {
        return localisation;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public String getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }

    public String getOrigin() {
        return origin;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(AlertType v) {
        this.type = v;
    }

    public void setLocalisation(Node v) {
        localisation = v;
    }

    public void setSeverity(AlertSeverity v) {
        severity = v;
    }

    public void setTime(String v) {
        time = v;
    }

    public void setStatus(String v) {
        status = v;
    }

    public void setOrigin(String v) {
        origin = v;
    }

    public boolean isSuggestion() {
        return "suggestion".equalsIgnoreCase(origin);
    }
}
