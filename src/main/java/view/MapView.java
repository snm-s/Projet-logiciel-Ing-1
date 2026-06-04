package view;

import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import model.zone.Zone;
import model.zone.ZoneUpdateListener;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Vue carte interactive avec Leaflet.js montrant les zones de Lyon
 * Colore dynamiquement les zones selon leur état d'inondation
 */
public class MapView implements ZoneUpdateListener {
    private WebView webView;
    private WebEngine webEngine;
    private List<Zone> zones;
    private Map<Integer, String> zoneColors;
    private static final double LYON_LAT = 45.7640;
    private static final double LYON_LNG = 4.8357;
    private static final int DEFAULT_ZOOM = 12;

    public MapView(List<Zone> zones) {
        this.zones = zones;
        this.zoneColors = new HashMap<>();
        this.webView = new WebView();
        this.webEngine = webView.getEngine();

        // Initialiser les couleurs par défaut
        for (Zone zone : zones) {
            zoneColors.put(zone.getId(), "#2ecc71"); // Vert par défaut
        }

        initializeMap();
    }

    public WebView getWebView() {
        return webView;
    }

    private void initializeMap() {
        String html = buildMapHtml();
        webEngine.loadContent(html);
    }

    private String buildMapHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<html><head>")
                .append("<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>")
                .append("<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>")
                .append("<style>")
                .append("html, body, #map { height: 100%; margin: 0; padding: 0; font-family: Arial, sans-serif; }")
                .append(".zone-label { font-size: 11px; font-weight: bold; color: white; text-shadow: 1px 1px 2px rgba(0,0,0,0.7); }")
                .append(".info { padding: 6px 8px; background: white; box-shadow: 0 0 15px rgba(0,0,0,0.2); border-radius: 5px; font-size: 12px; }")
                .append("</style>")
                .append("</head><body><div id='map'></div>")
                .append("<script>")
                .append("var map = L.map('map').setView([").append(LYON_LAT).append(", ").append(LYON_LNG).append("], ")
                .append(DEFAULT_ZOOM).append(");")
                .append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {")
                .append("  attribution: '© OpenStreetMap',")
                .append("  maxZoom: 19")
                .append("}).addTo(map);")
                .append("window.zones = {};");

        // Ajouter les cercles pour chaque zone
        for (Zone zone : zones) {
            String color = getColorForZone(zone);
            html.append("var circle_").append(zone.getId()).append(" = L.circle([")
                    .append(zone.getLatitude()).append(", ").append(zone.getLongitude()).append("], {")
                    .append("color: '").append(color).append("',")
                    .append("fillColor: '").append(color).append("',")
                    .append("fillOpacity: 0.6,")
                    .append("radius: 500,")
                    .append("weight: 2")
                    .append("}).addTo(map);")
                    .append("circle_").append(zone.getId()).append(".bindPopup('")
                    .append("<b>").append(zone.getName()).append("</b><br/>")
                    .append("Population: ").append(zone.getPopulation()).append("<br/>")
                    .append("Altitude: ").append(String.format("%.1f", zone.getAltitude())).append("m<br/>")
                    .append(zone.getDescription())
                    .append("');")
                    .append("window.zones[").append(zone.getId()).append("] = circle_").append(zone.getId())
                    .append(";");
        }

        html.append("</script></body></html>");
        return html.toString();
    }

    private String getColorForZone(Zone zone) {
        if (zone.isFlooded()) {
            return zone.isEvacuated() ? "#e74c3c" : "#f39c12"; // Rouge si inondé, orange si en cours d'évacuation
        }
        return "#2ecc71"; // Vert sinon
    }

    public void updateZoneColor(Zone zone) {
        String color = getColorForZone(zone);
        String script = String.format(
                "if (window.zones && window.zones[%d]) { window.zones[%d].setStyle({color: '%s', fillColor: '%s'}); }",
                zone.getId(), zone.getId(), color, color);
        webEngine.executeScript(script);
    }

    @Override
    public void onZoneFlooded(Zone zone) {
        updateZoneColor(zone);
    }

    @Override
    public void onZoneEvacuated(Zone zone) {
        updateZoneColor(zone);
    }

    @Override
    public void onZoneReset(Zone zone) {
        updateZoneColor(zone);
    }

    @Override
    public void onSimulationUpdated() {
        for (Zone zone : zones) {
            updateZoneColor(zone);
        }
    }

    public void updateAllZones(List<Zone> updatedZones) {
        this.zones = updatedZones;
        onSimulationUpdated();
    }
}
