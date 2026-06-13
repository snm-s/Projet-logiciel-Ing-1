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
    /**
     * Performs property.
     * @return the AlertType.
     */
    public AlertType typeProperty()         { return type; }
    /**
     * Performs property.
     * @return the StringProperty.
     */
    public StringProperty descriptionProperty()  { return description; }
    /**
     * Performs property.
     * @return the StringProperty.
     */
    public StringProperty localisationProperty() { return localisation; }
    /**
     * Performs property.
     * @return the StringProperty.
     */
    public StringProperty severityProperty()     { return severity; }
    /**
     * Performs property.
     * @return the StringProperty.
     */
    public StringProperty timeProperty()         { return time; }
    /**
     * Performs property.
     * @return the StringProperty.
     */
    public StringProperty statusProperty()       { return status; }
    /**
     * Performs property.
     * @return the StringProperty.
     */
    public StringProperty originProperty()       { return origin; }

    // Getters
    /**
     * Returns the id.
     * @return the int result.
     */
    public int    getId()           { return id; }
    /**
     * Returns the type.
     * @return the AlertType.
     */
    public AlertType getType()         { return type; }
    /**
     * Returns the description.
     * @return the String.
     */
    public String getDescription()  { return description.get(); }
    /**
     * Returns the localisation.
     * @return the String.
     */
    public String getLocalisation() { return localisation.get(); }
    /**
     * Returns the severity.
     * @return the String.
     */
    public String getSeverity()     { return severity.get(); }
    /**
     * Returns the time.
     * @return the String.
     */
    public String getTime()         { return time.get(); }
    /**
     * Returns the status.
     * @return the String.
     */
    public String getStatus()       { return status.get(); }
    /**
     * Returns the origin.
     * @return the String.
     */
    public String getOrigin()       { return origin.get(); }

    // Setters
    /**
     * Sets the id.
     * @param id the id.
     */
    public void setId(int id)              { this.id = id; }
    /**
     * Sets the description.
     * @param description the description.
     */
    public void setDescription(StringProperty description) {this.description = description;}
    /**
     * Sets the type.
     * @param v the v.
     */
    public void setType(AlertType v)          { this.type=v; }
    /**
     * Sets the description.
     * @param v the v.
     */
    public void setDescription(String v)   { description.set(v); }
    /**
     * Sets the localisation.
     * @param v the v.
     */
    public void setLocalisation(String v)  { localisation.set(v); }
    /**
     * Sets the severity.
     * @param v the v.
     */
    public void setSeverity(String v)      { severity.set(v); }
    /**
     * Sets the time.
     * @param v the v.
     */
    public void setTime(String v)          { time.set(v); }
    /**
     * Sets the status.
     * @param v the v.
     */
    public void setStatus(String v)        { status.set(v); }
    /**
     * Sets the origin.
     * @param v the v.
     */
    public void setOrigin(String v)        { origin.set(v); }

    /**
     * Returns whether suggestion.
     * @return the boolean result.
     */
    public boolean isSuggestion() { return "suggestion".equalsIgnoreCase(origin.get()); }
}
