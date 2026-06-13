package model.sensor;

import java.util.ArrayList;
import java.util.List;

import model.alert.AlertSystem;
import model.zone.Zone;
import model.zone.ZoneManager;

/**
 * WaterLevelDetector
 *
 * This class coordinates all water sensors deployed in the simulation.
 *
 * Responsibilities:
 * - Creates and maintains a collection of WaterSensor instances.
 * - Associates sensors with geographical zones.
 * - Updates water level measurements.
 * - Forwards measurements to the alert system.
 * - Notifies the map through callbacks when a zone changes.
 *
 * Design rationale:
 * This class acts as a mediator between the environmental sensing layer
 * (WaterSensor objects) and the decision-making layer (AlertSystem).
 *
 * In a real-world scenario, this component could be connected to
 * physical IoT flood sensors providing live measurements.
 */
public class WaterLevelDetector {

    /**
     * Collection of all water sensors managed by the detector.
     * Each sensor is associated with a specific zone.
     */
    private final List<WaterSensor> sensors = new ArrayList<>();

    /**
     * Alert system responsible for evaluating risk
     * and triggering warnings when thresholds are exceeded.
     */
    private final AlertSystem alertSystem;

    /**
     * Constructor.
     *
     * Initializes one sensor for each zone currently managed
     * by the ZoneManager.
     *
     * @param alertSystem The alert system used to process flood alerts.
     */
    public WaterLevelDetector(AlertSystem alertSystem) {

        this.alertSystem = alertSystem;

        /*
         * Simple initialization strategy:
         * one sensor is automatically deployed per zone.
         *
         * This assumption is sufficient for the simulation and
         * demonstrates how distributed environmental monitoring works.
         */
        ZoneManager zm = new ZoneManager();

        for (Zone z : zm.getZones()) {
            sensors.add(new WaterSensor(z));
        }
    }

    /**
     * Returns all registered water sensors.
     *
     * @return List of sensors currently monitored.
     */
    public List<WaterSensor> getSensors() {
        return sensors;
    }

    /**
     * Updates the measurement of a specific sensor.
     *
     * Workflow:
     * 1. Find the sensor associated with the given zone.
     * 2. Update its measured water level.
     * 3. Forward the information to the alert system.
     * 4. Notify the map using the callback mechanism.
     *
     * @param zoneId      Identifier of the target zone.
     * @param level       New measured water level.
     * @param mapCallback Callback used to refresh the visual map.
     */
    public void updateSensorForZone(
            int zoneId,
            double level,
            java.util.function.BiConsumer<Zone, Double> mapCallback) {

        sensors.stream()
                .filter(s -> s.getZone().getId() == zoneId)
                .findFirst()
                .ifPresent(s -> s.updateLevel(
                        level,
                        alertSystem,
                        mapCallback
                ));
    }

    /**
     * Updates all sensors simultaneously using externally supplied values.
     *
     * This method simulates a polling cycle in which measurements
     * are collected from all monitoring stations.
     *
     * Example:
     * levels[0] -> Zone 1
     * levels[1] -> Zone 2
     * etc.
     *
     * @param levels      Array containing the water levels.
     * @param mapCallback Callback used to update the map visualization.
     */
    public void updateAll(
            double[] levels,
            java.util.function.BiConsumer<Zone, Double> mapCallback) {

        for (int i = 0; i < sensors.size() && i < levels.length; i++) {

            sensors.get(i).updateLevel(
                    levels[i],
                    alertSystem,
                    mapCallback
            );
        }
    }

    /**
     * Generates random measurements for demonstration purposes.
     *
     * Intended usage:
     * - Testing the alert system.
     * - Demonstrating the simulation during presentations.
     * - Producing dynamic flood scenarios without real sensor data.
     *
     * Water levels are generated within a range of 0 to 12 meters.
     *
     * @param mapCallback Callback used to refresh the map display.
     */
    public void pollRandom(
            java.util.function.BiConsumer<Zone, Double> mapCallback) {

        for (WaterSensor s : sensors) {

            /*
             * Randomized flood level generation.
             *
             * Scale:
             * 0 m  → No flood risk.
             * 12 m → Severe flooding conditions.
             */
            double level = Math.random() * 12.0;

            s.updateLevel(
                    level,
                    alertSystem,
                    mapCallback
            );
        }
    }
}