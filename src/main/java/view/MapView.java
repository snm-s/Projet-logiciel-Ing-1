package view;

import controller.MapController;
import model.observer.Observer;
import model.zone.Zone;
import model.zone.ZoneUpdateListener;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.*;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * Vue carte interactive utilisant JXMapViewer2 + tuiles OpenStreetMap.
 * Affiche des zones colorées (polygones délimités) et des routes inter-zones colorées.
 *
 * Mêmes signatures de méthodes que l'ancienne MapView WebView.
 */
public class MapView implements ZoneUpdateListener, Observer<Zone> {

    // ─── Carte ───────────────────────────────────────────────────────────
    private final JXMapViewer mapViewer;
    private final SwingNode swingNode;
    private List<Zone>        zones;

    // ─── Painters (couches graphiques) ───────────────────────────────────
    private ZonePainter      zonePainter;
    private RoutePainter     routePainter;
    private CompoundPainter<JXMapViewer> compound;

    // ─── Controller (optionnel) ───────────────────────────────────────────
    private MapController mapController;

    // ─── Callback sélection ──────────────────────────────────────────────
    private Consumer<Zone> onZoneSelected;

    // ─── Constantes Lyon ─────────────────────────────────────────────────
    private static final GeoPosition LYON_CENTER = new GeoPosition(45.7640, 4.8357);
    private static final int         DEFAULT_ZOOM = 6;   // JXMapViewer : 17-zoom

    // =========================================================================
    public MapView(List<Zone> zones) {
        this.zones     = zones;
        this.mapViewer = new JXMapViewer();
        this.swingNode = new SwingNode();
        configureTiles();
        configurePainters();
        configureInteraction();
        loadZones();
        swingNode.setContent(mapViewer);
        Platform.runLater(() -> {
            mapViewer.repaint();
            mapViewer.revalidate();
        });

    }

    // ─────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────

    private void configureTiles() {
        // 1. Forcez un User-Agent valide avant toute requête réseau
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // 2. Utilisez une source de tuiles sécurisée (HTTPS)
        TileFactoryInfo info = new OSMTileFactoryInfo("OSM", "https://tile.openstreetmap.org");
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        
        tileFactory.setThreadPoolSize(8);
        mapViewer.setTileFactory(tileFactory);
        // Force une taille minimale dès le départ
        mapViewer.setPreferredSize(new Dimension(800, 600));
        mapViewer.setMinimumSize(new Dimension(400, 300));
        mapViewer.setZoom(DEFAULT_ZOOM);
        mapViewer.setAddressLocation(LYON_CENTER);
    }

    private void configurePainters() {
        zonePainter  = new ZonePainter(zones);
        routePainter = new RoutePainter(zones);

        compound = new CompoundPainter<>();
        compound.addPainter(routePainter);  // routes en dessous
        compound.addPainter(zonePainter);   // zones par-dessus
        mapViewer.setOverlayPainter(compound);
    }

    private void configureInteraction() {
        // Pan avec la souris
        PanMouseInputListener panListener = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(panListener);
        mapViewer.addMouseMotionListener(panListener);

        // Zoom molette
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        // Clic sur zone
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMapClick(e.getPoint());
            }
        });
    }

    private void loadZones() {
        zonePainter.setZones(zones);
        routePainter.setZones(zones);
        mapViewer.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACCESSEURS
    // ─────────────────────────────────────────────────────────────────────

    /** Retourne le composant Swing à intégrer dans l'application. */
    public JXMapViewer getMapViewer() { return mapViewer; }

    public SwingNode getSwingNode() {
        return swingNode;
    }

    /** Compatibilité — retourne null (plus de WebView). */
    public Object getWebView()   { return null; }
    public Object getWebEngine() { return null; }

    public void setMapController(MapController controller) {
        this.mapController = controller;
    }

    public void setOnZoneSelected(Consumer<Zone> callback) {
        this.onZoneSelected = callback;
    }

    // ─────────────────────────────────────────────────────────────────────
    // MISE À JOUR DYNAMIQUE
    // ─────────────────────────────────────────────────────────────────────

    /** Met à jour la couleur d'une zone selon son état inondé/évacué. */
    public void updateZoneColor(Zone zone) {
        SwingUtilities.invokeLater(() -> {
            zonePainter.updateZone(zone);
            mapViewer.repaint();
        });
    }

    /** Met à jour une zone avec son niveau d'eau réel + rafraîchit les routes. */
    public void updateZoneWithWaterLevel(Zone zone, double niveauEau) {
        SwingUtilities.invokeLater(() -> {
            zonePainter.updateZoneWaterLevel(zone, niveauEau);
            routePainter.setZones(zones);
            mapViewer.repaint();
        });
    }

    /** Rafraîchit uniquement les couleurs des routes. */
    public void refreshRouteColors() {
        SwingUtilities.invokeLater(() -> {
            routePainter.setZones(zones);
            mapViewer.repaint();
        });
    }

    /** Met à jour toutes les zones d'un coup. */
    public void updateAllZones(List<Zone> updatedZones) {
        this.zones = updatedZones;
        SwingUtilities.invokeLater(() -> {
            zonePainter.setZones(updatedZones);
            routePainter.setZones(updatedZones);
            mapViewer.repaint();
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────────────────────

    public void zoomIn() {
        SwingUtilities.invokeLater(() -> {
            int z = mapViewer.getZoom();
            if (z > 1) mapViewer.setZoom(z - 1);
        });
    }

    public void zoomOut() {
        SwingUtilities.invokeLater(() -> {
            int z = mapViewer.getZoom();
            if (z < 17) mapViewer.setZoom(z + 1);
        });
    }

    public void setZoom(int level) {
        SwingUtilities.invokeLater(() -> mapViewer.setZoom(level));
    }

    public void resetView() {
        SwingUtilities.invokeLater(() -> {
            mapViewer.setAddressLocation(LYON_CENTER);
            mapViewer.setZoom(DEFAULT_ZOOM);
        });
    }

    public void flyTo(double lat, double lng) {
        SwingUtilities.invokeLater(() -> {
            mapViewer.setAddressLocation(new GeoPosition(lat, lng));
            mapViewer.setZoom(2);
        });
    }

    public void panTo(double lat, double lng) {
        SwingUtilities.invokeLater(() ->
            mapViewer.setAddressLocation(new GeoPosition(lat, lng)));
    }

    public void focusZone(Zone zone) {
        if (zone == null) return;
        SwingUtilities.invokeLater(() -> {
            mapViewer.setAddressLocation(new GeoPosition(zone.getLatitude(), zone.getLongitude()));
            mapViewer.setZoom(2);
            selectZone(zone);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // SÉLECTION
    // ─────────────────────────────────────────────────────────────────────

    public void selectZone(Zone zone) {
        if (zone == null) return;
        zonePainter.setSelectedZone(zone);
        mapViewer.repaint();
        if (onZoneSelected != null) onZoneSelected.accept(zone);
    }

    public void highlightZone(int id) {
        zones.stream()
             .filter(z -> z.getId() == id)
             .findFirst()
             .ifPresent(this::selectZone);
    }

    // ─────────────────────────────────────────────────────────────────────
    // GESTION DU CLIC CARTE
    // ─────────────────────────────────────────────────────────────────────

    private void handleMapClick(Point screenPoint) {
        GeoPosition clickPos = mapViewer.convertPointToGeoPosition(screenPoint);
        double lat = clickPos.getLatitude();
        double lng = clickPos.getLongitude();

        // Trouver la zone la plus proche du clic (rayon ~500 m ≈ 0.0045°)
        Zone closest = null;
        double minDist = Double.MAX_VALUE;

        for (Zone z : zones) {
            double dLat = z.getLatitude()  - lat;
            double dLng = z.getLongitude() - lng;
            double dist = Math.sqrt(dLat * dLat + dLng * dLng);
            if (dist < 0.007 && dist < minDist) {
                minDist = dist;
                closest = z;
            }
        }

        if (closest != null) {
            selectZone(closest);
            zonePainter.showPopup(closest, mapViewer, screenPoint);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // INTERFACES Observer / ZoneUpdateListener
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
        refreshRouteColors();
    }
}
