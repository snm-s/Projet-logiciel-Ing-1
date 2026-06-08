package controller;

import model.zone.Zone;
import view.MapView;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller de la carte — mêmes signatures publiques qu'avant,
 * mais délègue directement à MapView (Swing/JXMapViewer) sans WebEngine.
 *
 * Tous les appels sont thread-safe grâce à SwingUtilities.invokeLater().
 */
public class MapController {

    private final MapView    mapView;
    private final List<Zone> zones;

    private Consumer<Zone> onZoneSelected;
    private Zone           selectedZone;

    private static final double DEFAULT_LAT  = 45.7640;
    private static final double DEFAULT_LNG  = 4.8357;
    private static final int    DEFAULT_ZOOM = 4;
    private static final int    ZONE_ZOOM    = 2;

    // ─────────────────────────────────────────────────────────────────────
    public MapController(MapView mapView, List<Zone> zones) {
        this.mapView = mapView;
        this.zones   = zones;

        // Brancher le callback de sélection
        mapView.setOnZoneSelected(zone -> {
            this.selectedZone = zone;
            if (onZoneSelected != null) onZoneSelected.accept(zone);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────────────────────

    public void zoomIn()            { mapView.zoomIn(); }
    public void zoomOut()           { mapView.zoomOut(); }
    public void setZoom(int level)  { mapView.setZoom(level); }
    public void resetView()         { mapView.resetView(); }
    public void flyTo(double lat, double lng)  { mapView.flyTo(lat, lng); }
    public void panTo(double lat, double lng)  { mapView.panTo(lat, lng); }

    public void focusZone(Zone zone) {
        if (zone == null) return;
        mapView.focusZone(zone);
    }

    public void focusZoneById(int id) {
        zones.stream()
             .filter(z -> z.getId() == id)
             .findFirst()
             .ifPresent(this::focusZone);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SÉLECTION
    // ─────────────────────────────────────────────────────────────────────

    public void selectZone(Zone zone) {
        if (zone == null) return;
        this.selectedZone = zone;
        mapView.selectZone(zone);
        if (onZoneSelected != null)
            SwingUtilities.invokeLater(() -> onZoneSelected.accept(zone));
    }

    public Zone getSelectedZone() { return selectedZone; }

    // ─────────────────────────────────────────────────────────────────────
    // MISE À JOUR
    // ─────────────────────────────────────────────────────────────────────

    public void updateZone(Zone zone, double niveauEau) {
        mapView.updateZoneWithWaterLevel(zone, niveauEau);
    }

    public void updateAllZones(List<Zone> updatedZones) {
        mapView.updateAllZones(updatedZones);
    }

    // ─────────────────────────────────────────────────────────────────────
    // ROUTES
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Dessine des routes custom entre les zones (paires d'IDs).
     * Si routes = null ou vide, revient au réseau par défaut.
     */
    public void drawRoutes(List<int[]> routes, List<Zone> zones) {
        mapView.updateAllZones(zones);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACK
    // ─────────────────────────────────────────────────────────────────────

    public void setOnZoneSelected(Consumer<Zone> callback) {
        this.onZoneSelected = callback;
    }

    // ─────────────────────────────────────────────────────────────────────
    // HIGHLIGHT (compatibilité ancienne API)
    // ─────────────────────────────────────────────────────────────────────

    public void highlightZone(int id) {
        mapView.highlightZone(id);
    }

    /**
     * Affiche un popup d'infos sur une zone (compatibilité ancienne API).
     * Le HTML n'est pas utilisé — on affiche une boîte Swing.
     */
    public void showZonePopup(int id, String htmlContent) {
        zones.stream()
             .filter(z -> z.getId() == id)
             .findFirst()
             .ifPresent(zone -> SwingUtilities.invokeLater(() ->
                 JOptionPane.showMessageDialog(
                     mapView.getMapViewer(),
                     "<html>" + htmlContent + "</html>",
                     zone.getName(),
                     JOptionPane.INFORMATION_MESSAGE)));
    }
}
