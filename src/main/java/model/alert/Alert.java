package model.alert;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import model.enums.AlertType;

public class Alert {
    private int id;
    private AlertType type;
    private StringProperty description;


    private final StringProperty localisation;
    private final StringProperty severity;
    private final StringProperty time;
    private final StringProperty status;   // "Active", "Résolue", "En attente"
    private final StringProperty origin;   // "admin" | "suggestion"

    public Alert(model.enums.AlertType evacuation, String description, String localisation,
                 String severity, String time, String status) {
        this(evacuation, description, localisation, severity, time, status, "admin");
    }

    public Alert(AlertType type, String description, String localisation,
                 String severity, String time, String status, String origin) {
        this.id           = 0;
        this.type         = type;
        this.description  = new SimpleStringProperty(description);
        this.localisation = new SimpleStringProperty(localisation);
        this.severity     = new SimpleStringProperty(severity);
        this.time         = new SimpleStringProperty(time);
        this.status       = new SimpleStringProperty(status);
        this.origin       = new SimpleStringProperty(origin);
    }

    // JavaFX Properties
    public AlertType typeProperty()         { return type; }
    public StringProperty descriptionProperty()  { return description; }
    public StringProperty localisationProperty() { return localisation; }
    public StringProperty severityProperty()     { return severity; }
    public StringProperty timeProperty()         { return time; }
    public StringProperty statusProperty()       { return status; }
    public StringProperty originProperty()       { return origin; }

    // Getters
    public int    getId()           { return id; }
    public AlertType getType()         { return type; }
    public String getDescription()  { return description.get(); }
    public String getLocalisation() { return localisation.get(); }
    public String getSeverity()     { return severity.get(); }
    public String getTime()         { return time.get(); }
    public String getStatus()       { return status.get(); }
    public String getOrigin()       { return origin.get(); }

    // Setters
    public void setId(int id)              { this.id = id; }
    public void setDescription(StringProperty description) {this.description = description;}
    public void setType(AlertType v)          { this.type=v; }
    public void setDescription(String v)   { description.set(v); }
    public void setLocalisation(String v)  { localisation.set(v); }
    public void setSeverity(String v)      { severity.set(v); }
    public void setTime(String v)          { time.set(v); }
    public void setStatus(String v)        { status.set(v); }
    public void setOrigin(String v)        { origin.set(v); }

    public boolean isSuggestion() { return "suggestion".equalsIgnoreCase(origin.get()); }
}
