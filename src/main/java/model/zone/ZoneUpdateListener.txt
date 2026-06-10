package model.zone;

/**
 * Listener pour les mises à jour d'état des zones
 */
public interface ZoneUpdateListener {
    void onZoneFlooded(Zone zone);

    void onZoneEvacuated(Zone zone);

    void onZoneReset(Zone zone);

    void onSimulationUpdated();
}
