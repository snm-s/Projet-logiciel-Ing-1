package controller;

import model.graph.Edge;
import model.graph.RouteGraph;
import model.observer.Observer;
import model.zone.Zone;
import view.MapView;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Contrôleur principal de la carte (pattern MVC strict).
 *
 * <h3>Responsabilités</h3>
 * <ul>
 *   <li>Possède le {@link RouteGraph} (couche Model).</li>
 *   <li>Délègue l'affichage à {@link MapView} (couche View).</li>
 *   <li>Écoute les mises à jour de zones et recalcule les routes.</li>
 *   <li>Appelle {@link RouteGraph#simulateFlows()} pour mettre à jour les flux
 *       après chaque changement d'état des zones.</li>
 * </ul>
 *
 * <h3>Pipeline d'initialisation</h3>
 * <ol>
 *   <li>{@link RouteGraph} se construit, charge le JSON et récupère les tracés OSRM.</li>
 *   <li>Le graphe est injecté dans {@link MapView#setRouteGraph(RouteGraph)} →
 *       {@link view.RoutePainter} s'abonne aux edges.</li>
 *   <li>Les mises à jour de zones passent par {@link #update(Zone)} ou
 *       {@link #updateZone(Zone, double)}.</li>
 * </ol>
 */
public class MapController implements Observer<Zone> {

    // ─── MVC ─────────────────────────────────────────────────────────────
    private final MapView    mapView;
    private final RouteGraph routeGraph;
    private final List<Zone> zones;

    // ─── Sélection ───────────────────────────────────────────────────────
    private Consumer<Zone> onZoneSelected;
    private Zone           selectedZone;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Construit le contrôleur, initialise le graphe depuis les fichiers et
     * l'API OSRM, puis injecte le graphe dans la vue.
     *
     * @param mapView vue JXMapViewer
     * @param zones   liste vivante des zones de la simulation
     */
    public MapController(MapView mapView, List<Zone> zones) {
        this.mapView    = mapView;
        this.zones      = zones;

        // Construction du graphe (charge JSON + OSRM)
        this.routeGraph = new RouteGraph(zones);

        // Injection dans le RoutePainter via la vue
        mapView.setRouteGraph(routeGraph);

        // Callback sélection depuis la vue
        mapView.setOnZoneSelected(zone -> {
            this.selectedZone = zone;
            if (onZoneSelected != null) onZoneSelected.accept(zone);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Observer<Zone>
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Appelé par le modèle quand une zone change d'état.
     * Rafraîchit les edges associés et recalcule les flux d'évacuation.
     */
    @Override
    public void update(Zone zone) {
        if (zone == null) return;

        // Mettre à jour les edges concernés
        for (Edge edge : routeGraph.getEdgesForZone(zone)) {
            edge.refreshState();
        }

        // Recalculer les flux (évacuations, congestion…)
        routeGraph.simulateFlows();

        // Mettre à jour la vue
        mapView.updateZoneColor(zone);
    }

    // ─────────────────────────────────────────────────────────────────────
    // MISE À JOUR
    // ─────────────────────────────────────────────────────────────────────

    /** Met à jour une zone avec son niveau d'eau et recalcule les routes. */
    public void updateZone(Zone zone, double niveauEau) {
        mapView.updateZoneWithWaterLevel(zone, niveauEau);
        for (Edge edge : routeGraph.getEdgesForZone(zone)) {
            edge.refreshState();
        }
        routeGraph.simulateFlows();
    }

    /** Rafraîchit toutes les zones et tous les edges (fin de simulation, reset…). */
    public void updateAllZones(List<Zone> updatedZones) {
        mapView.updateAllZones(updatedZones);
        routeGraph.refreshAllEdges();
        routeGraph.simulateFlows();
    }

    // ─────────────────────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────────────────────

    public void zoomIn()                       { mapView.zoomIn(); }
    public void zoomOut()                      { mapView.zoomOut(); }
    public void setZoom(int level)             { mapView.setZoom(level); }
    public void resetView()                    { mapView.resetView(); }
    public void flyTo(double lat, double lng)  { mapView.flyTo(lat, lng); }
    public void panTo(double lat, double lng)  { mapView.panTo(lat, lng); }

    public void focusZone(Zone zone) {
        if (zone != null) mapView.focusZone(zone);
    }

    public void focusZoneById(int id) {
        zones.stream().filter(z -> z.getId() == id)
             .findFirst().ifPresent(this::focusZone);
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

    public Zone           getSelectedZone()                   { return selectedZone; }
    public void           setOnZoneSelected(Consumer<Zone> cb){ this.onZoneSelected = cb; }
    public void           highlightZone(int id)               { mapView.highlightZone(id); }
    public RouteGraph     getRouteGraph()                     { return routeGraph; }

    public void showZonePopup(int id, String htmlContent) {
        zones.stream().filter(z -> z.getId() == id).findFirst()
             .ifPresent(zone -> SwingUtilities.invokeLater(() ->
                 JOptionPane.showMessageDialog(
                     mapView.getMapViewer(),
                     "<html>" + htmlContent + "</html>",
                     zone.getName(),
                     JOptionPane.INFORMATION_MESSAGE)));
    }
}