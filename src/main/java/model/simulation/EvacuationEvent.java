package model.simulation;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * EvacuationEvent
 *
 * This class represents an event occurring during the evacuation simulation.
 *
 * Responsibilities:
 * - Store a chronological trace of important simulation events.
 * - Record which agent generated the event.
 * - Describe what happened and where it occurred.
 * - Provide a structured history that can be displayed
 *   in dashboards, logs, or simulation reports.
 *
 * Examples of events:
 * - A citizen starts evacuating.
 * - An agent reaches a shelter.
 * - A rescue operation begins.
 * - A rescue operation is completed.
 * - A route is changed due to congestion or flooding.
 *
 * Design rationale:
 * Rather than storing plain text logs, events are represented
 * as objects containing structured information.
 * This makes filtering, searching, statistics generation,
 * and UI visualization much easier.
 */
public class EvacuationEvent {

    /**
     * Unique identifier of the agent involved in the event.
     */
    private final int agentId;

    /**
     * Human-readable name of the agent.
     */
    private final String agentName;

    /**
     * Timestamp indicating when the event occurred.
     *
     * Format:
     * HH:mm (e.g., 14:32)
     */
    private final String time;

    /**
     * Category of the event.
     *
     * Examples:
     * - INFO
     * - WARNING
     * - EVACUATION
     * - RESCUE
     * - ARRIVAL
     */
    private final String type;

    /**
     * Detailed description of the event.
     */
    private final String message;

    /**
     * Origin zone associated with the event.
     *
     * Example:
     * The zone from which the citizen evacuated.
     */
    private final String fromZone;

    /**
     * Destination zone associated with the event.
     *
     * Example:
     * The shelter reached by the citizen.
     */
    private final String toZone;

    /**
     * Creates a new evacuation event.
     *
     * Defensive programming choices:
     * - Null values are replaced by meaningful defaults.
     * - Agent names are automatically generated if missing.
     * - Event timestamps are captured at creation time.
     *
     * @param agentId   Identifier of the involved agent.
     * @param agentName Display name of the agent.
     * @param type      Event category.
     * @param message   Event description.
     * @param fromZone  Origin zone.
     * @param toZone    Destination zone.
     */
    public EvacuationEvent(
            int agentId,
            String agentName,
            String type,
            String message,
            String fromZone,
            String toZone) {

        this.agentId = agentId;

        /*
         * Generate a default name if none is provided.
         * This guarantees that every event remains readable.
         */
        this.agentName = agentName == null
                ? "Agent #" + agentId
                : agentName;

        /*
         * Default event category.
         */
        this.type = type == null
                ? "INFO"
                : type;

        /*
         * Avoid null descriptions.
         */
        this.message = message == null
                ? ""
                : message;

        /*
         * Avoid null zone values.
         */
        this.fromZone = fromZone == null
                ? ""
                : fromZone;

        this.toZone = toZone == null
                ? ""
                : toZone;

        /*
         * Capture the exact creation time of the event.
         *
         * Using LocalTime ensures that events can be
         * displayed chronologically during the simulation.
         */
        this.time = LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * Returns the identifier of the involved agent.
     */
    public int getAgentId() {
        return agentId;
    }

    /**
     * Returns the display name of the involved agent.
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * Returns the timestamp of the event.
     */
    public String getTime() {
        return time;
    }

    /**
     * Returns the category of the event.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the event description.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the origin zone.
     */
    public String getFromZone() {
        return fromZone;
    }

    /**
     * Returns the destination zone.
     */
    public String getToZone() {
        return toZone;
    }
}