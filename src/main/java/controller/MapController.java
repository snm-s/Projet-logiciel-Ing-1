package controller;

import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.enums.CitizenState;
import model.graph.AgentMovement;
import model.algorithms.EvacuationPath;
import model.graph.RouteGraph;
import model.zone.Zone;
import view.MapView;

import javafx.application.Platform;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller de la carte — orchestre MapView, RouteGraph, et les déplacements d'agents.
 *
 * <h3>Rôles :</h3>
 * <ul>
 *   <li>Navigation carte (zoom, flyTo, focusZone…)</li>
 *   <li>Construction du RouteGraph et injection dans MapView</li>
 *   <li>Déclenchement des évacuations (agents Citizen et RescueAgent)</li>
 *   <li>Propagation des ticks de simulation vers RouteGraph</li>
 * </ul>
 */
public class MapController {

    private final MapView      mapView;
    private final List<Zone>   zones;
    private final RouteGraph   routeGraph;

    private Consumer<Zone>     onZoneSelected;
    private Consumer<AgentMovement> onAgentArrived;
    private Zone               selectedZone;

    private static final int ZONE_ZOOM = 2;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTEUR
    // ─────────────────────────────────────────────────────────────────────

    /**
     * @param mapView vue carte (déjà instanciée)
     * @param zones   liste complète des zones
     * @param agents  tous les agents de la simulation (pour planifier les évacuations)
     */
    public MapController(MapView mapView, List<Zone> zones, List<Agent> agents) {
        this.mapView = mapView;
        this.zones   = zones;

        // Construire le graphe (peut faire des appels OSRM réseau)
        this.routeGraph = new RouteGraph(zones);

        // Injecter le graphe dans la vue
        mapView.setRouteGraph(routeGraph);
        mapView.setMapController(this);

        // Brancher le callback sélection zone → controller
        mapView.setOnZoneSelected(zone -> {
            this.selectedZone = zone;
            if (onZoneSelected != null) Platform.runLater(() -> onZoneSelected.accept(zone));
        });

        // Listener arrivées / blocages d'agents
        routeGraph.addArrivalListener(new RouteGraph.ArrivalListener() {
            @Override
            public void onAgentArrived(AgentMovement mv) {
                handleAgentArrived(mv);
            }
            @Override
            public void onAgentBlocked(AgentMovement mv) {
                handleAgentBlocked(mv);
            }
        });
    }

    /** Constructeur sans agents (rétro-compatibilité). */
    public MapController(MapView mapView, List<Zone> zones) {
        this(mapView, zones, List.of());
    }

    // ─────────────────────────────────────────────────────────────────────
    // TICK SIMULATION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Appelé à chaque pas de simulation par le SimulationController.
     * Avance tous les agents et rafraîchit l'affichage.
     *
     * @param deltaSeconds durée du pas simulé
     */
    public void tick(double deltaSeconds) {
        // 1. Mettre à jour flux et états des arêtes
        routeGraph.refreshAllEdges();
        routeGraph.simulateFlows();

        // 2. Avancer les agents
        routeGraph.tick(deltaSeconds);

        // 3. Demander le repaint (agents ont bougé + routes ont changé)
        mapView.refreshRouteColors();
    }

    // ─────────────────────────────────────────────────────────────────────
    // ÉVACUATION D'AGENTS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Planifie l'évacuation d'un citoyen depuis la zone donnée.
     * Calcule le chemin optimal (Dijkstra multi-critère) et démarre le mouvement.
     *
     * @param citizen citoyen à évacuer
     * @param from    zone actuelle du citoyen (doit être inondée ou à risque)
     * @return le mouvement créé, ou null si aucun chemin
     */
    public AgentMovement evacuateCitizen(Citizen citizen, Zone from) {
        if (citizen == null || from == null) return null;
        AgentMovement mv = routeGraph.planEvacuation(citizen, from, zones);
        if (mv != null) {
            citizen.setState(CitizenState.ESCAPING);
            mapView.refreshRouteColors();
        }
        return mv;
    }

    /**
     * Envoie un secouriste vers une zone cible.
     *
     * @param agent  secouriste
     * @param from   zone de départ (position actuelle)
     * @param target zone à atteindre (pour secourir)
     * @return le mouvement créé, ou null si aucun chemin
     */
    public AgentMovement sendRescueAgent(RescueAgent agent, Zone from, Zone target) {
        if (agent == null || from == null || target == null) return null;
        AgentMovement mv = routeGraph.planRescueMission(agent, from, target);
        if (mv != null) mapView.refreshRouteColors();
        return mv;
    }

    /**
     * Déclenche l'évacuation de masse de tous les citoyens dans les zones inondées.
     * Appelé automatiquement par le SimulationController quand le niveau monte.
     *
     * @param citizens tous les citoyens de la simulation
     */
    public void triggerMassEvacuation(List<Citizen> citizens) {
        for (Zone zone : zones) {
            if (!zone.isFlooded()) continue;
            for (Citizen c : citizens) {
                if (c.getState() == CitizenState.ESCAPING
                 || c.getState() == CitizenState.SAFE) continue;

                // On vérifie si le citoyen est dans cette zone (via position ou zone assignée)
                // Si aucun nœud de position, on prend la zone directement
                evacuateCitizen(c, zone);
            }
        }
    }

    /**
     * Calcule le chemin d'évacuation recommandé depuis une zone
     * (sans déplacer d'agent — pour affichage dans l'UI).
     */
    public EvacuationPath computeEvacuationPreview(Zone from) {
        return routeGraph.getRouter().findNearestSafe(from, zones);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACKS ARRIVÉES
    // ─────────────────────────────────────────────────────────────────────

    private void handleAgentArrived(AgentMovement mv) {
        Agent agent = mv.getAgent();
        if (agent instanceof Citizen c) {
            c.setState(CitizenState.SAFE);
        }
        if (onAgentArrived != null)
            Platform.runLater(() -> onAgentArrived.accept(mv));
        mapView.refreshRouteColors();
    }

    private void handleAgentBlocked(AgentMovement mv) {
        // Le RouteGraph se charge de la replanification automatique
        mapView.refreshRouteColors();
    }

    // ─────────────────────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────────────────────

    public void zoomIn()                          { mapView.zoomIn(); }
    public void zoomOut()                         { mapView.zoomOut(); }
    public void setZoom(int level)                { mapView.setZoom(level); }
    public void resetView()                       { mapView.resetView(); }
    public void flyTo(double lat, double lng)     { mapView.flyTo(lat, lng); }
    public void panTo(double lat, double lng)     { mapView.panTo(lat, lng); }
    public void highlightZone(int id)             { mapView.highlightZone(id); }

    public void focusZone(Zone zone) {
        if (zone == null) return;
        mapView.focusZone(zone);
        selectZone(zone);
    }

    public void focusZoneById(int id) {
        zones.stream().filter(z -> z.getId() == id).findFirst().ifPresent(this::focusZone);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SÉLECTION
    // ─────────────────────────────────────────────────────────────────────

    public void selectZone(Zone zone) {
        if (zone == null) return;
        this.selectedZone = zone;
        mapView.selectZone(zone);
    }

    public Zone getSelectedZone() { return selectedZone; }

    // ─────────────────────────────────────────────────────────────────────
    // MISE À JOUR ZONES
    // ─────────────────────────────────────────────────────────────────────

    public void updateZone(Zone zone, double niveauEau) {
        mapView.updateZoneWithWaterLevel(zone, niveauEau);
    }

    public void updateAllZones(List<Zone> updatedZones) {
        mapView.updateAllZones(updatedZones);
        routeGraph.refreshAllEdges();
    }

    // ─────────────────────────────────────────────────────────────────────
    // COMPATIBILITÉ drawRoutes (ancienne API)
    // ─────────────────────────────────────────────────────────────────────

    public void drawRoutes(List<int[]> routes, List<Zone> zones) {
        // Le routage est désormais entièrement géré par RouteGraph
        mapView.refreshRouteColors();
    }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACKS
    // ─────────────────────────────────────────────────────────────────────

    public void setOnZoneSelected(Consumer<Zone> cb)          { this.onZoneSelected = cb; }
    public void setOnAgentArrived(Consumer<AgentMovement> cb) { this.onAgentArrived = cb; }

    // ─────────────────────────────────────────────────────────────────────
    // ACCESSEURS
    // ─────────────────────────────────────────────────────────────────────

    public RouteGraph  getRouteGraph()  { return routeGraph; }
    public List<Zone>  getZones()       { return zones; }
}