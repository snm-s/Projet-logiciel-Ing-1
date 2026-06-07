package model.sensor;

import model.alert.AlertSystem;
import model.zone.Zone;
import model.zone.ZoneManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordonne plusieurs capteurs et fournit des méthodes d'actualisation.
 */
public class WaterLevelDetector {
    private final List<WaterSensor> sensors = new ArrayList<>();
    private final AlertSystem alertSystem;

    public WaterLevelDetector(AlertSystem alertSystem) {
        this.alertSystem = alertSystem;
        // Initialisation simple : créer un capteur par zone
        ZoneManager zm = new ZoneManager();
        for (Zone z : zm.getZones())
            sensors.add(new WaterSensor(z));
    }

    public List<WaterSensor> getSensors() {
        return sensors;
    }

    /** Mise à jour ponctuelle d'un capteur par zone id. */
    public void updateSensorForZone(int zoneId, double level, java.util.function.BiConsumer<Zone, Double> mapCallback) {
        sensors.stream().filter(s -> s.getZone().getId() == zoneId).findFirst()
                .ifPresent(s -> s.updateLevel(level, alertSystem, mapCallback));
    }

    /**
     * Polling simplifié : met à jour tous les capteurs avec des valeurs fournies.
     */
    public void updateAll(double[] levels, java.util.function.BiConsumer<Zone, Double> mapCallback) {
        for (int i = 0; i < sensors.size() && i < levels.length; i++) {
            sensors.get(i).updateLevel(levels[i], alertSystem, mapCallback);
        }
    }

    /** Génère des mesures aléatoires (utilitaire de test/demo). */
    public void pollRandom(java.util.function.BiConsumer<Zone, Double> mapCallback) {
        for (WaterSensor s : sensors) {
            double level = Math.random() * 12.0; // échelle 0..12m
            s.updateLevel(level, alertSystem, mapCallback);
        }
    }
}
