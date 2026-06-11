package model.sensor;

import model.zone.Zone;
import model.alert.AlertSystem;
import model.enums.AlertSeverity;
import model.enums.AlertType;
import model.alert.Alert;

/**
 * Capteur de niveau d'eau attaché à une Zone.
 * Met à jour l'état de la zone, notifie l'AlertSystem et le MapView optionnel.
 */
public class WaterSensor {
    private final Zone zone;
    private double level;

    public WaterSensor(Zone zone) {
        this.zone = zone;
        this.level = 0.0;
    }

    public Zone getZone() {
        return zone;
    }

    public double getLevel() {
        return level;
    }

    /**
     * Met à jour le niveau d'eau mesuré par le capteur. Si le niveau dépasse
     * l'altitude de la zone, la zone est marquée inondée et une alerte est créée.
     *
     * @param newLevel    nouveau niveau d'eau
     * @param alertSystem système d'alertes (peut être null)
     * @param mapCallback optionnel callback MapView via une interface fonctionnelle
     */
    public void updateLevel(double newLevel, AlertSystem alertSystem,
            java.util.function.BiConsumer<Zone, Double> mapCallback) {
        this.level = newLevel;
        boolean previouslyFlooded = zone.isFlooded();

        if (newLevel >= zone.getAltitude()) {
            zone.setFlooded(true);
            if (!previouslyFlooded && alertSystem != null) {
                String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                alertSystem.addAlert(new Alert(
                        AlertType.INONDATION,
                        "Capteur: inondation détectée dans " + zone.getName(),
                        zone.getName(),
                        AlertSeverity.HIGH,
                        time,
                        "Active",
                        "sensor"));
            }
        } else {
            if (previouslyFlooded) {
                zone.setFlooded(false);
                if (alertSystem != null) {
                    String time = java.time.LocalTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    alertSystem.addAlert(new Alert(
                            AlertType.INFO,
                            "Capteur: niveau revenu sous le seuil pour " + zone.getName(),
                            zone.getName(),
                            AlertSeverity.LOW,
                            time,
                            "Résolue",
                            "sensor"));
                }
            }
        }

        if (mapCallback != null) {
            try {
                mapCallback.accept(zone, newLevel);
            } catch (Exception ignored) {
            }
        }
    }
}
