package model.sensor;

import model.alert.Alert;
import model.alert.AlertSystem;
import model.enums.AlertType;
import model.zone.Zone;

/**
 * WaterSensor
 *
 * This class represents a water level sensor associated with a specific zone.
 *
 * Responsibilities:
 * - Continuously monitor the water level of its assigned zone.
 * - Compare measured values against the zone's altitude threshold.
 * - Update the flood status of the zone.
 * - Notify the AlertSystem whenever a flood starts or ends.
 * - Inform the map layer through an optional callback mechanism.
 *
 * Design rationale:
 * Each sensor behaves as an independent monitoring device.
 * This architecture mimics real-world IoT flood sensors deployed
 * across different geographical areas.
 */
public class WaterSensor {

    /**
     * Zone monitored by this sensor.
     *
     * The relationship is one-to-one:
     * one sensor is attached to one zone.
     */
    private final Zone zone;

    /**
     * Latest measured water level.
     */
    private double level;

    /**
     * Creates a sensor associated with a given zone.
     *
     * Initial conditions:
     * - The monitored water level is set to zero.
     *
     * @param zone Zone monitored by this sensor.
     */
    public WaterSensor(Zone zone) {
        this.zone = zone;
        this.level = 0.0;
    }

    /**
     * Returns the monitored zone.
     *
     * @return Associated zone.
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * Returns the latest measured water level.
     *
     * @return Current water level.
     */
    public double getLevel() {
        return level;
    }

    /**
     * Updates the water level measured by the sensor.
     *
     * Workflow:
     * 1. Store the new measurement.
     * 2. Determine whether the zone becomes flooded.
     * 3. Trigger flood alerts if thresholds are crossed.
     * 4. Trigger recovery alerts if the situation improves.
     * 5. Refresh the map visualization through the callback.
     *
     * Flood decision rule:
     * A zone is considered flooded when:
     *
     *      water level >= zone altitude
     *
     * This simplified model assumes that altitude represents
     * the flooding threshold of the area.
     *
     * @param newLevel    Newly measured water level.
     * @param alertSystem Alert manager used to create alerts.
     *                    May be null.
     * @param mapCallback Optional callback used to refresh the map.
     */
    public void updateLevel(
            double newLevel,
            AlertSystem alertSystem,
            java.util.function.BiConsumer<Zone, Double> mapCallback) {

        /*
         * Store the latest sensor reading.
         */
        this.level = newLevel;

        /*
         * Save the previous state in order to detect transitions.
         *
         * This avoids generating duplicate alerts while the zone
         * remains continuously flooded.
         */
        boolean previouslyFlooded = zone.isFlooded();

        /*
         * FLOOD DETECTION
         *
         * If the measured water level exceeds the zone altitude,
         * the zone enters a flooded state.
         */
        if (newLevel >= zone.getAltitude()) {

            zone.setFlooded(true);

            /*
             * Generate an alert only when the flooding state changes.
             */
            if (!previouslyFlooded && alertSystem != null) {

                String time = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

                alertSystem.addAlert(new Alert(
                        AlertType.INONDATION,
                        "Sensor detected flooding in " + zone.getName(),
                        zone.getName(),
                        "High",
                        time,
                        "Active",
                        "sensor"
                ));
            }

        } else {

            /*
             * RECOVERY DETECTION
             *
             * The water level has dropped below the flooding threshold.
             */
            if (previouslyFlooded) {

                zone.setFlooded(false);

                /*
                 * Inform the system that the critical situation
                 * has been resolved.
                 */
                if (alertSystem != null) {

                    String time = java.time.LocalTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

                    alertSystem.addAlert(new Alert(
                            AlertType.INFO,
                            "Water level returned below the threshold for " + zone.getName(),
                            zone.getName(),
                            "Low",
                            time,
                            "Resolved",
                            "sensor"
                    ));
                }
            }
        }

        /*
         * Notify the map layer.
         *
         * The callback updates the graphical representation of the zone,
         * for example by changing its color or displaying the water level.
         *
         * A try-catch block is used to prevent UI failures from affecting
         * the simulation engine.
         */
        if (mapCallback != null) {
            try {
                mapCallback.accept(zone, newLevel);

            } catch (Exception ignored) {

                /*
                 * Visualization errors should not interrupt
                 * the flood monitoring process.
                 */
            }
        }
    }
}