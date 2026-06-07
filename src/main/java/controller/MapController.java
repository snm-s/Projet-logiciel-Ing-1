package controller;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import model.zone.Zone;
import view.MapView;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller de la carte Leaflet.
 * Gère : zoom, déplacement (flyTo / panTo), sélection de zone au clic,
 * affichage popup d'infos, et mise à jour visuelle dynamique.
 *
 * Utilise le pont Java ↔ JavaScript via WebEngine.executeScript().
 */
public class MapController {

    private final MapView    mapView;
    private final WebEngine  webEngine;
    private final List<Zone> zones;

    // Callback déclenché quand l'utilisateur clique sur une zone
    private Consumer<Zone> onZoneSelected;

    // Zone actuellement sélectionnée
    private Zone selectedZone;

    // Coordonnées par défaut (Lyon)
    private static final double DEFAULT_LAT  = 45.7640;
    private static final double DEFAULT_LNG  = 4.8357;
    private static final int    DEFAULT_ZOOM = 13;
    private static final int    ZONE_ZOOM    = 15;

    // ─────────────────────────────────────────────────────────────────────
    public MapController(MapView mapView, List<Zone> zones) {
        this.mapView   = mapView;
        this.webEngine = extractWebEngine(mapView);
        this.zones     = zones;
    }

    // ─────────────────────────────────────────────────────────────────────
    // NAVIGATION — ZOOM
    // ─────────────────────────────────────────────────────────────────────

    /** Zoome in d'un niveau. */
    public void zoomIn() {
        executeScript("map.zoomIn();");
    }

    /** Dézoome d'un niveau. */
    public void zoomOut() {
        executeScript("map.zoomOut();");
    }

    /** Définit un niveau de zoom précis. */
    public void setZoom(int level) {
        executeScript("map.setZoom(" + level + ");");
    }

    /** Recentre sur Lyon avec le zoom par défaut. */
    public void resetView() {
        executeScript(String.format(
            "map.flyTo([%f, %f], %d, {animate:true, duration:0.8});",
            DEFAULT_LAT, DEFAULT_LNG, DEFAULT_ZOOM));
    }

    // ─────────────────────────────────────────────────────────────────────
    // NAVIGATION — DÉPLACEMENT
    // ─────────────────────────────────────────────────────────────────────

    /** Déplace la vue vers des coordonnées données, avec animation fluide. */
    public void flyTo(double lat, double lng) {
        executeScript(String.format(
            "map.flyTo([%f, %f], %d, {animate:true, duration:0.6});",
            lat, lng, ZONE_ZOOM));
    }

    /** Déplace la vue vers des coordonnées données, sans animation. */
    public void panTo(double lat, double lng) {
        executeScript(String.format("map.panTo([%f, %f]);", lat, lng));
    }

    /** Déplace la vue vers une zone spécifique. */
    public void focusZone(Zone zone) {
        if (zone == null) return;
        executeScript(String.format(
            "map.flyTo([%f, %f], %d, {animate:true, duration:0.7});",
            zone.getLatitude(), zone.getLongitude(), ZONE_ZOOM));
        selectZone(zone);
    }

    /** Déplace la vue vers une zone par son id. */
    public void focusZoneById(int id) {
        zones.stream()
             .filter(z -> z.getId() == id)
             .findFirst()
             .ifPresent(this::focusZone);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SÉLECTION DE ZONE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sélectionne une zone : met en surbrillance sur la carte,
     * affiche un popup d'infos détaillées, et appelle le callback.
     */
    public void selectZone(Zone zone) {
        if (zone == null) return;
        this.selectedZone = zone;

        // Mettre en surbrillance la zone sur la carte
        executeScript(String.format("window.highlightZone(%d);", zone.getId()));

        // Afficher un popup Leaflet avec les infos complètes
        String popupHtml = buildZonePopupHtml(zone);
        executeScript(String.format(
            "window.showZonePopup(%d, '%s');",
            zone.getId(), escapeJs(popupHtml)));

        // Notifier la vue Java
        if (onZoneSelected != null) {
            Platform.runLater(() -> onZoneSelected.accept(zone));
        }
    }

    public Zone getSelectedZone() {
        return selectedZone;
    }

    // ─────────────────────────────────────────────────────────────────────
    // POPUP D'INFOS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Construit le HTML du popup affiché sur la carte quand on clique
     * sur une zone. Reprend le thème dark du dashboard.
     */
    private String buildZonePopupHtml(Zone zone) {
        String statusColor = zone.isFlooded() ? "#ef4444" : zone.isEvacuated() ? "#f59e0b" : "#22c55e";
        String statusText  = zone.isFlooded() ? "⚠ Inondée" : zone.isEvacuated() ? "↗ Évacuation" : "✓ Sûre";
        String altColor    = zone.getAltitude() < 1.0 ? "#ef4444" : zone.getAltitude() < 2.0 ? "#f59e0b" : "#22c55e";

        return "<div style='font-family:monospace;min-width:200px;'>" +
               "<div style='font-size:14px;font-weight:900;color:#f1f5f9;margin-bottom:8px;border-bottom:1px solid #1e293b;padding-bottom:6px;'>" +
               escapeHtml(zone.getName()) + "</div>" +

               "<div style='display:flex;justify-content:space-between;margin-bottom:4px;'>" +
               "<span style='color:#94a3b8;font-size:11px;'>Statut</span>" +
               "<span style='color:" + statusColor + ";font-size:11px;font-weight:bold;'>" + statusText + "</span></div>" +

               "<div style='display:flex;justify-content:space-between;margin-bottom:4px;'>" +
               "<span style='color:#94a3b8;font-size:11px;'>Population</span>" +
               "<span style='color:#f1f5f9;font-size:11px;font-weight:bold;'>" + zone.getPopulation() + " hab.</span></div>" +

               "<div style='display:flex;justify-content:space-between;margin-bottom:4px;'>" +
               "<span style='color:#94a3b8;font-size:11px;'>Altitude</span>" +
               "<span style='color:" + altColor + ";font-size:11px;font-weight:bold;'>" +
               String.format("%.1f m", zone.getAltitude()) + "</span></div>" +

               "<div style='margin-top:6px;padding:6px;background:#0d1117;border-radius:4px;'>" +
               "<span style='color:#94a3b8;font-size:10px;'>" + escapeHtml(zone.getDescription()) + "</span></div>" +

               "<div style='display:flex;justify-content:space-between;margin-top:6px;'>" +
               "<span style='color:#94a3b8;font-size:10px;'>Lat: " + String.format("%.4f", zone.getLatitude()) + "</span>" +
               "<span style='color:#94a3b8;font-size:10px;'>Lng: " + String.format("%.4f", zone.getLongitude()) + "</span></div>" +
               "</div>";
    }

    // ─────────────────────────────────────────────────────────────────────
    // MISE À JOUR DES ZONES
    // ─────────────────────────────────────────────────────────────────────

    /** Met à jour la couleur/état d'une zone sur la carte. */
    public void updateZone(Zone zone, double niveauEau) {
        Platform.runLater(() ->
            mapView.updateZoneWithWaterLevel(zone, niveauEau));
    }

    /** Met à jour toutes les zones d'un coup. */
    public void updateAllZones(List<Zone> updatedZones) {
        Platform.runLater(() -> mapView.updateAllZones(updatedZones));
    }

    // ─────────────────────────────────────────────────────────────────────
    // ROUTES (arêtes colorées entre zones)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Dessine toutes les routes inter-zones avec une couleur adaptée
     * au statut (sûre = vert, risque = orange, inondée = rouge).
     * Appeler après le chargement de la carte.
     */
    public void drawRoutes(List<int[]> routes, List<Zone> zones) {
        if (routes == null || routes.isEmpty()) return;

        // Nettoyer les routes existantes
        executeScript("window.clearRoutes && window.clearRoutes();");

        for (int[] edge : routes) {
            if (edge.length < 2) continue;

            Zone from = findZoneById(zones, edge[0]);
            Zone to   = findZoneById(zones, edge[1]);
            if (from == null || to == null) continue;

            // Couleur selon l'état des deux zones
            String color = routeColor(from, to);
            String style = routeStyle(from, to);

            String script = String.format(
                "window.drawRoute([%f,%f],[%f,%f],'%s','%s');",
                from.getLatitude(), from.getLongitude(),
                to.getLatitude(),   to.getLongitude(),
                color, style);
            executeScript(script);
        }
    }

    private String routeColor(Zone from, Zone to) {
        if (from.isFlooded() || to.isFlooded())   return "#ef4444"; // rouge = inondée
        double avgAlt = (from.getAltitude() + to.getAltitude()) / 2.0;
        if (avgAlt < 1.0) return "#f59e0b"; // orange = risque
        return "#22c55e"; // vert = sûre
    }

    private String routeStyle(Zone from, Zone to) {
        if (from.isFlooded() && to.isFlooded()) return "flooded";
        if (from.isFlooded() || to.isFlooded()) return "risk";
        return "safe";
    }

    private Zone findZoneById(List<Zone> zoneList, int id) {
        return zoneList.stream().filter(z -> z.getId() == id).findFirst().orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACK
    // ─────────────────────────────────────────────────────────────────────

    /** Appelé quand l'utilisateur clique sur une zone dans la carte. */
    public void setOnZoneSelected(Consumer<Zone> callback) {
        this.onZoneSelected = callback;
    }

    // ─────────────────────────────────────────────────────────────────────
    // UTILITAIRES PRIVÉS
    // ─────────────────────────────────────────────────────────────────────

    private void executeScript(String script) {
        if (webEngine == null) return;
        Platform.runLater(() -> {
            try {
                webEngine.executeScript(script);
            } catch (Exception e) {
                System.err.println("[MapController] Script error: " + e.getMessage());
            }
        });
    }

    /** Extrait le WebEngine depuis un MapView via réflexion sur le WebView. */
    private WebEngine extractWebEngine(MapView mv) {
        try {
            javafx.scene.web.WebView wv = mv.getWebView();
            return wv.getEngine();
        } catch (Exception e) {
            System.err.println("[MapController] Impossible d'extraire le WebEngine: " + e.getMessage());
            return null;
        }
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'",  "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
}
