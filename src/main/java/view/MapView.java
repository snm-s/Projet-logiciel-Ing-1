package view;

import controller.MapController;
import model.graph.AgentMovement;
import model.graph.RouteGraph;
import model.observer.Observer;
import model.zone.Zone;
import model.zone.ZoneUpdateListener;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.VirtualEarthTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.viewer.*;

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
 * Vue carte JXMapViewer2 intégrée dans JavaFX via {@link SwingNode}.
 *
 * <h3>Ordre d'initialisation obligatoire :</h3>
 * <ol>
 *   <li>Appeler {@link #MapView(List)} depuis n'importe quel thread</li>
 *   <li>Ajouter {@link #getSwingNode()} à la scène JavaFX <em>sur le thread FX</em></li>
 *   <li>Injecter le graphe via {@link #setRouteGraph(RouteGraph)}</li>
 *   <li>Injecter le controller via {@link #setMapController(MapController)}</li>
 * </ol>
 *
 * <p>Tous les appels de mise à jour sont thread-safe :
 * ils délèguent automatiquement au thread Swing via {@link SwingUtilities#invokeLater}.
 */
public class MapView implements ZoneUpdateListener, Observer<Zone> {

    // ─── Composants carte ─────────────────────────────────────────────────
    private final JXMapViewer mapViewer;
    private final SwingNode   swingNode;
    private List<Zone>        zones;

    // ─── Painters (couches graphiques empilées) ───────────────────────────
    private final ZonePainter   zonePainter;
    private final RoutePainter  routePainter;
    private final AgentPainter  agentPainter;
    private final CompoundPainter<JXMapViewer> compound;

    // ─── Références ───────────────────────────────────────────────────────
    private MapController      mapController;
    private Consumer<Zone>     onZoneSelected;

    // ─── Constantes ───────────────────────────────────────────────────────
    private static final GeoPosition LYON_CENTER = new GeoPosition(45.7640, 4.8357);
    private static final int         DEFAULT_ZOOM = 6;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTEUR
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Crée la MapView. Peut être appelé depuis n'importe quel thread.
     * Le {@link SwingNode} est initialisé ici et doit être ajouté
     * à la scène JavaFX ultérieurement.
     */
    public MapView(List<Zone> zones) {
        this.zones = zones;

        // ── 1. Construire le JXMapViewer (doit être sur le thread Swing) ──
        this.mapViewer = new JXMapViewer();
        this.swingNode = new SwingNode();

        // Painters créés indépendamment des threads
        this.zonePainter  = new ZonePainter(zones);
        this.routePainter = new RoutePainter();
        this.agentPainter = new AgentPainter();

        this.compound = new CompoundPainter<>();
        compound.addPainter(routePainter);
        compound.addPainter(zonePainter);
        compound.addPainter(agentPainter);

        // ── 2. Initialiser le JXMapViewer sur le thread Swing ──
        SwingUtilities.invokeLater(() -> {
            initMapViewer();
            swingNode.setContent(mapViewer);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // INITIALISATION INTERNE (thread Swing)
    // ─────────────────────────────────────────────────────────────────────

    private void initMapViewer() {
        // User-agent pour les tuiles OSM
        // 1. Utilisez VirtualEarth (Bing Maps) au lieu d'OSM
            // Cela évite les restrictions de filtrage d'OpenStreetMap
        TileFactoryInfo info = new VirtualEarthTileFactoryInfo(VirtualEarthTileFactoryInfo.MAP);
        DefaultTileFactory tf = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tf);



        mapViewer.setPreferredSize(new Dimension(800, 600));
        mapViewer.setMinimumSize(new Dimension(200, 200));
        mapViewer.setZoom(DEFAULT_ZOOM);
        mapViewer.setAddressLocation(LYON_CENTER);
        mapViewer.setOverlayPainter(compound);

        // Interaction : panoramique
        PanMouseInputListener pan = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(pan);
        mapViewer.addMouseMotionListener(pan);
        // Zoom molette
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
        // Clic pour sélection de zone
        mapViewer.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { handleClick(e.getPoint()); }
        });

        zonePainter.setZones(zones);
    }

    // ─────────────────────────────────────────────────────────────────────
    // INJECTION GRAPHE DE ROUTES
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Injecte le graphe de routes dans les painters.
     * Doit être appelé après la construction, avant le premier tick.
     */
    public void setRouteGraph(RouteGraph graph) {
        routePainter.setRouteGraph(graph);
        agentPainter.setRouteGraph(graph);
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACCESSEURS
    // ─────────────────────────────────────────────────────────────────────

    /** Nœud JavaFX à insérer dans la scène. */
    public SwingNode   getSwingNode()    { return swingNode; }

    /** Composant Swing sous-jacent (pour SwingUtilities). */
    public JXMapViewer getMapViewer()    { return mapViewer; }

    /** Compatibilité ancienne API. */
    public Object      getWebView()      { return null; }
    public Object      getWebEngine()    { return null; }

    public void setMapController(MapController c)     { this.mapController = c; }
    public void setOnZoneSelected(Consumer<Zone> cb)  { this.onZoneSelected = cb; }

    // ─────────────────────────────────────────────────────────────────────
    // MISE À JOUR ZONES (thread-safe)
    // ─────────────────────────────────────────────────────────────────────

    public void updateZoneColor(Zone zone) {
        SwingUtilities.invokeLater(() -> {
            zonePainter.updateZone(zone);
            mapViewer.repaint();
        });
    }

    public void updateZoneWithWaterLevel(Zone zone, double niveauEau) {
        SwingUtilities.invokeLater(() -> {
            zonePainter.updateZoneWaterLevel(zone, niveauEau);
            mapViewer.repaint();
        });
    }

    /**
     * Rafraîchit l'affichage des routes et agents.
     * Appelé après chaque tick de simulation.
     */
    public void refreshRouteColors() {
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    public void updateAllZones(List<Zone> updatedZones) {
        this.zones = updatedZones;
        SwingUtilities.invokeLater(() -> {
            zonePainter.setZones(updatedZones);
            mapViewer.repaint();
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // NAVIGATION (thread-safe)
    // ─────────────────────────────────────────────────────────────────────

    public void zoomIn() {
        SwingUtilities.invokeLater(() -> {
            int z = mapViewer.getZoom(); if (z > 1) mapViewer.setZoom(z - 1);
        });
    }

    public void zoomOut() {
        SwingUtilities.invokeLater(() -> {
            int z = mapViewer.getZoom(); if (z < 17) mapViewer.setZoom(z + 1);
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
            mapViewer.setAddressLocation(
                new GeoPosition(zone.getLatitude(), zone.getLongitude()));
            mapViewer.setZoom(3);
        });
        selectZone(zone);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SÉLECTION
    // ─────────────────────────────────────────────────────────────────────

    public void selectZone(Zone zone) {
        if (zone == null) return;
        SwingUtilities.invokeLater(() -> {
            zonePainter.setSelectedZone(zone);
            mapViewer.repaint();
        });
        if (onZoneSelected != null) Platform.runLater(() -> onZoneSelected.accept(zone));
    }

    public void highlightZone(int id) {
        zones.stream().filter(z -> z.getId() == id).findFirst().ifPresent(this::selectZone);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CLIC CARTE
    // ─────────────────────────────────────────────────────────────────────

    private void handleClick(Point screenPoint) {
        GeoPosition click = mapViewer.convertPointToGeoPosition(screenPoint);
        double lat = click.getLatitude(), lng = click.getLongitude();

        Zone closest = null;
        double minDist = Double.MAX_VALUE;
        for (Zone z : zones) {
            double d = Math.hypot(z.getLatitude() - lat, z.getLongitude() - lng);
            if (d < 0.007 && d < minDist) { minDist = d; closest = z; }
        }
        if (closest != null) {
            selectZone(closest);
            zonePainter.showPopup(closest, mapViewer, screenPoint);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // INTERFACES Observer / ZoneUpdateListener
    // ─────────────────────────────────────────────────────────────────────

    @Override public void update(Zone zone)           { if (zone != null) updateZoneColor(zone); }
    @Override public void onZoneFlooded(Zone zone)    { updateZoneColor(zone); }
    @Override public void onZoneEvacuated(Zone zone)  { updateZoneColor(zone); }
    @Override public void onZoneReset(Zone zone)      { updateZoneColor(zone); }

    @Override
    public void onSimulationUpdated() {
        for (Zone z : zones) updateZoneColor(z);
        refreshRouteColors();
    }
}