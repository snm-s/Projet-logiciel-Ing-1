package model.simulation;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EvacuationEvent {
    private final int agentId;
    private final String agentName;
    private final String time;
    private final String type;
    private final String message;
    private final String fromZone;
    private final String toZone;

    public EvacuationEvent(int agentId, String agentName, String type, String message, String fromZone, String toZone) {
        this.agentId = agentId;
        this.agentName = agentName == null ? "Agent #" + agentId : agentName;
        this.type = type == null ? "INFO" : type;
        this.message = message == null ? "" : message;
        this.fromZone = fromZone == null ? "" : fromZone;
        this.toZone = toZone == null ? "" : toZone;
        this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public int getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public String getTime() { return time; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getFromZone() { return fromZone; }
    public String getToZone() { return toZone; }
}
