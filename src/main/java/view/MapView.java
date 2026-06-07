package view;

import controller.MapController;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import model.observer.Observer;
import model.zone.Zone;
import model.zone.ZoneUpdateListener;

import java.util.List;

/**
 * Vue carte interactive Leaflet.js — thème dark, zones colorées selon le niveau d'eau.
 * 
 * MISE À JOUR : expose getWebEngine() pour MapController,
 * et intègre refreshRouteColors() après chaque mise à jour de zone.
 */
public class MapView implements ZoneUpdateListener, Observer<Zone> {

    private final WebView   webView;
    private final WebEngine webEngine;
    private List<Zone> zones;

    // MapController optionnel (injecté après création)
    private MapController mapController;

    // Centre Lyon
    private static final double LYON_LAT    = 45.7640;
    private static final double LYON_LNG    = 4.8357;
    private static final int    DEFAULT_ZOOM = 13;

    // ─────────────────────────────────────────────────────────────────────
    public MapView(List<Zone> zones) {
        this.zones   = zones;
        this.webView = new WebView();
        this.webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.webView.setPrefSize(800, 600);
        this.webView.setMinWidth(400);
        this.webView.setMinHeight(300);
        this.webEngine = webView.getEngine();
        initializeMap();
    }

    public WebView   getWebView()   { return webView; }
    public WebEngine getWebEngine() { return webEngine; }

    /** Injecte le MapController après construction (évite la dépendance circulaire). */
    public void setMapController(MapController controller) {
        this.mapController = controller;
    }

    // ─────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────
    private void initializeMap() {
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                System.out.println("Carte chargée ! Injection des zones...");
                String jsonData = zonesToJson();
                // Remplacer les apostrophes qui cassent le JS
                String safeJson = jsonData.replace("'", "\\'");
                String script = "window.loadAllZones('" + safeJson + "');";
                webEngine.executeScript(script);
            }
        });

        String url = getClass().getResource("/map/map.html").toExternalForm();
        webEngine.load(url);
    }

    // ─────────────────────────────────────────────────────────────────────
    // MISE À JOUR DYNAMIQUE
    // ─────────────────────────────────────────────────────────────────────

    public void updateZoneColor(Zone zone) {
        Platform.runLater(() -> {
            String script = String.format(
                "window.updateZone(%d, %b, %b, %.2f);",
                zone.getId(), zone.isFlooded(), zone.isEvacuated(),
                zone.isFlooded() ? 1.5 : 0.0);
            try { webEngine.executeScript(script); } catch (Exception ignored) {}
        });
    }

    /** Mise à jour avec niveau d'eau réel + rafraîchissement des routes. */
    public void updateZoneWithWaterLevel(Zone zone, double niveauEau) {
        Platform.runLater(() -> {
            String script = String.format(
                "window.updateZone(%d,%b,%b,%.2f);",
                zone.getId(), zone.isFlooded(), zone.isEvacuated(), niveauEau);
            try {
                webEngine.executeScript(script);
                // Rafraîchir les routes après mise à jour
                refreshRouteColors();
            } catch (Exception ignored) {}
        });
    }

    /** Rafraîchit les couleurs des routes selon l'état actuel des zones. */
    public void refreshRouteColors() {
        String json = zonesToJsonForRoutes();
        String safe = json.replace("'", "\\'");
        try {
            webEngine.executeScript("window.refreshRouteColors('" + safe + "');");
        } catch (Exception ignored) {}
    }

    public void updateAllZones(List<Zone> updatedZones) {
        this.zones = updatedZones;
        Platform.runLater(() -> {
            for (Zone zone : zones) {
                String script = String.format(
                    "window.updateZone(%d,%b,%b,%.2f);",
                    zone.getId(), zone.isFlooded(), zone.isEvacuated(),
                    zone.isFlooded() ? 1.5 : 0.0);
                try { webEngine.executeScript(script); } catch (Exception ignored) {}
            }
            refreshRouteColors();
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // INTERFACES
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void update(Zone zone) {
        if (zone != null) updateZoneColor(zone);
    }

    @Override
    public void onZoneFlooded(Zone zone)   { updateZoneColor(zone); }

    @Override
    public void onZoneEvacuated(Zone zone) { updateZoneColor(zone); }

    @Override
    public void onZoneReset(Zone zone)     { updateZoneColor(zone); }

    @Override
    public void onSimulationUpdated() {
        for (Zone zone : zones) updateZoneColor(zone);
        Platform.runLater(this::refreshRouteColors);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SÉRIALISATION JSON
    // ─────────────────────────────────────────────────────────────────────

    private String zonesToJson() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            String name = z.getName().replaceAll("[^a-zA-Z0-9 ]", "");
            String desc = z.getDescription().replaceAll("[^a-zA-Z0-9 ]", "");
            json.append(String.format(
                "{\"id\":%d,\"lat\":%f,\"lng\":%f,\"name\":\"%s\"," +
                "\"pop\":%d,\"alt\":%f,\"desc\":\"%s\",\"flooded\":%b}",
                z.getId(), z.getLatitude(), z.getLongitude(), name,
                z.getPopulation(), z.getAltitude(), desc, z.isFlooded()));
            if (i < zones.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    /** JSON allégé pour le rafraîchissement des routes. */
    private String zonesToJsonForRoutes() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            json.append(String.format(
                "{\"id\":%d,\"lat\":%f,\"lng\":%f,\"alt\":%f,\"flooded\":%b}",
                z.getId(), z.getLatitude(), z.getLongitude(),
                z.getAltitude(), z.isFlooded()));
            if (i < zones.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    // ─────────────────────────────────────────────────────────────────────
    // UTILITAIRE
    // ─────────────────────────────────────────────────────────────────────
    private String escapeJs(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("'",  "\\'")
                  .replace("\"", "\\\"");
    }
}
