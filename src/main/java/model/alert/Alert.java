
package model.alert;

public class Alert {
    private int id;
    private String message;
    private int urgencyLevel;

    public Alert(int id, String message, int urgencyLevel) {
        this.id = id;
        this.message = message;
        this.urgencyLevel = urgencyLevel;
    }

    public String getMessage() { return message; }
    public int getId() { return id; }
public int getUrgencyLevel() { return urgencyLevel; }
}
