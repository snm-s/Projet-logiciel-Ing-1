package controller.AdminPage;

import controller.MapController;
import model.agent.Citizen;
import model.graph.AgentMovement;
import model.simulation.FloodSimulation;
import model.zone.Zone;
import model.graph.EdgeState;

import javafx.application.Platform;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Controller principal de la simulation d'inondation.
 *
 * <p>Changements par rapport à la version précédente :
 * <ul>
 *   <li>Intègre le {@link MapController} : chaque {@link #executerPas()} appelle
 *       {@code mapController.tick()} pour faire avancer les agents sur la carte.</li>
 *   <li>Déclenche l'évacuation de masse automatiquement quand le niveau d'eau
 *       dépasse le seuil critique ({@value #SEUIL_EVACUATION_M}).</li>
 *   <li>Expose des callbacks supplémentaires pour les arrivées d'agents.</li>
 * </ul>
 */
public class SimulationController {

    private final FloodSimulation modele;
    private double vitesseSimulationMs;

    /** Seuil de niveau d'eau (en mètres) déclenchant l'évacuation automatique. */
    private static final double SEUIL_EVACUATION_M = 0.5;

    /** Durée d'un pas simulé en secondes (pour RouteGraph.tick). */
    private static final double DELTA_SECONDS = 1.0;

    // ─── Callbacks ────────────────────────────────────────────────────────
    private Consumer<String>      onStatusChanged;
    private Consumer<Double>      onWaterLevelChanged;
    private Consumer<List<Zone>>  onZonesUpdated;
    private Consumer<AgentMovement> onAgentArrived;

    // ─── Mode ─────────────────────────────────────────────────────────────
    private boolean modeAleatoire = true;

    // ─── MapController (injecté après construction) ───────────────────────
    private MapController mapController;

    // ─── État interne ─────────────────────────────────────────────────────
    private boolean evacuationDeclenchee = false;

    // ─────────────────────────────────────────────────────────────────────
    public SimulationController() {
        this.modele = new FloodSimulation();
        this.vitesseSimulationMs = 500.0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // INJECTION DU MAP CONTROLLER
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Injecte le MapController après sa création (évite la dépendance circulaire).
     * Branche aussi le callback d'arrivée d'agents.
     */
    public void setMapController(MapController mc) {
        this.mapController = mc;
        mc.setOnAgentArrived(mv -> {
            if (onAgentArrived != null)
                Platform.runLater(() -> onAgentArrived.accept(mv));
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACCÈS AU MODÈLE
    // ─────────────────────────────────────────────────────────────────────

    public FloodSimulation getModele()             { return modele; }
    public List<Zone>      getZones()              { return modele.getZones(); }

    public Zone getZoneById(int id) {
        return modele.getZones().stream()
            .filter(z -> z.getId() == id).findFirst().orElse(null);
    }

    public Zone getZoneByName(String name) {
        return modele.getZones().stream()
            .filter(z -> z.getName() != null && z.getName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONTRÔLES SIMULATION
    // ─────────────────────────────────────────────────────────────────────

    public void demarrerSimulation() {
        modele.demarrer();
        evacuationDeclenchee = false;
        notifyStatus("Simulation en cours");
    }

    public void mettreEnPause() {
        modele.setEnPause(true);
        notifyStatus("En pause");
    }

    public void reprendreSimulation() {
        modele.setEnPause(false);
        notifyStatus("Simulation en cours");
    }

    /**
     * Exécute un pas de simulation complet :
     * <ol>
     *   <li>Avance le modèle d'inondation</li>
     *   <li>Vérifie le seuil d'évacuation automatique</li>
     *   <li>Avance tous les agents sur la carte (tick MapController)</li>
     *   <li>Notifie les listeners UI</li>
     * </ol>
     */
    public void executerPas() {
        if (modele.isEnPause()) return;

        // 1. Avancer la simulation d'inondation
        modele.executerPas();

        double niveau = modele.getNiveauEau();

        // 2. Déclencher l'évacuation si seuil atteint
        if (!evacuationDeclenchee && niveau >= SEUIL_EVACUATION_M) {
            declencherEvacuationAutomatique();
            evacuationDeclenchee = true;
        }

        // 3. Avancer les agents sur la carte
        if (mapController != null) {
            mapController.tick(DELTA_SECONDS);
            // Mettre à jour les couleurs des zones sur la carte
            mapController.updateAllZones(modele.getZones());
        }

        // 4. Notifier l'UI (sur le thread JavaFX)
        Platform.runLater(() -> {
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(niveau);
            if (onZonesUpdated != null)      onZonesUpdated.accept(modele.getZones());
        });
    }

    /**
     * Déclenche l'évacuation de tous les citoyens dans les zones inondées.
     */
    private void declencherEvacuationAutomatique() {
        if (mapController == null) return;

        List<Citizen> citizens = modele.getAgents().stream()
            .filter(a -> a instanceof Citizen)
            .map(a -> (Citizen) a)
            .collect(Collectors.toList());

        mapController.triggerMassEvacuation(citizens);
        notifyStatus("⚠ Évacuation déclenchée !");
    }

    public void resetSimulation() {
        modele.resetSimulation();
        evacuationDeclenchee = false;
        notifyStatus("Simulation réinitialisée");
        Platform.runLater(() -> {
            if (onZonesUpdated      != null) onZonesUpdated.accept(modele.getZones());
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(0.0);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // PARAMÈTRES
    // ─────────────────────────────────────────────────────────────────────

    public void setVitesseSimulation(double ms) { this.vitesseSimulationMs = Math.max(100.0, ms); }
    public double getVitesseSimulationMs()       { return vitesseSimulationMs; }

    public void setGravite(double g) { modele.setGravite(g); }

    public void setNiveauEau(double nv) {
        modele.setNiveauEau(nv);
        Platform.runLater(() -> { if (onWaterLevelChanged != null) onWaterLevelChanged.accept(nv); });
    }

    // ─────────────────────────────────────────────────────────────────────
    // MODE
    // ─────────────────────────────────────────────────────────────────────

    public void setModeAleatoire(boolean b) { this.modeAleatoire = b; }
    public boolean isModeAleatoire()         { return modeAleatoire; }

    // ─────────────────────────────────────────────────────────────────────
    // STATISTIQUES
    // ─────────────────────────────────────────────────────────────────────

    public int getPopulationARisque() {
        return modele.getZones().stream().filter(Zone::isFlooded)
            .mapToInt(Zone::getPopulation).sum();
    }

    public int getPopulationEnSecurite() {
        return modele.getZones().stream().filter(z -> !z.isFlooded())
            .mapToInt(Zone::getPopulation).sum();
    }

    public long getNombreZonesInondees() {
        return modele.getZones().stream().filter(Zone::isFlooded).count();
    }

    public long getNombreZonesSures() {
        return modele.getZones().stream().filter(z -> !z.isFlooded()).count();
    }

    /** Agents actifs en déplacement sur la carte. */
    public int getNombreAgentsEnDeplacement() {
        if (mapController == null) return 0;
        return mapController.getRouteGraph().getActiveMovements().size();
    }

    public double[] getStatutReseau() {
        if (mapController == null) {
            double nv = modele.getNiveauEau();
            if (nv <= 0) return new double[]{100.0, 0.0, 0.0, 0.0, 0.0};
            double inondee = Math.min(nv / 3.0 * 100.0, 60.0);
            double risque  = Math.min(nv / 2.0 * 30.0, 30.0);
            return new double[]{Math.max(100.0 - inondee - risque, 0.0), risque, 0.0, 0.0, inondee};
        }
        
        // Calculer depuis l'état réel des arêtes du graphe
        var edges = mapController.getRouteGraph().getEdges();
        if (edges.isEmpty()) return new double[]{100.0, 0.0, 0.0, 0.0, 0.0};
        
        double total = edges.size();
        
        long safe       = edges.stream().filter(e -> e.getState() == EdgeState.SAFE).count();
        long risk       = edges.stream().filter(e -> e.getState() == EdgeState.AT_RISK).count();
        long congested  = edges.stream().filter(e -> e.getState() == EdgeState.CONGESTED).count();
        long overloaded = edges.stream().filter(e -> e.getState() == EdgeState.OVERLOADED).count();
        long flooded    = edges.stream().filter(e -> e.getState() == EdgeState.FLOODED).count();
        
        return new double[]{
            safe       * 100.0 / total,
            risk       * 100.0 / total,
            congested  * 100.0 / total,
            overloaded * 100.0 / total,
            flooded    * 100.0 / total
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACKS
    // ─────────────────────────────────────────────────────────────────────

    public void setOnStatusChanged(Consumer<String> cb)         { this.onStatusChanged = cb; }
    public void setOnWaterLevelChanged(Consumer<Double> cb)     { this.onWaterLevelChanged = cb; }
    public void setOnZonesUpdated(Consumer<List<Zone>> cb)      { this.onZonesUpdated = cb; }
    public void setOnAgentArrived(Consumer<AgentMovement> cb)   { this.onAgentArrived = cb; }

    // ─────────────────────────────────────────────────────────────────────

    private void notifyStatus(String status) {
        if (onStatusChanged != null)
            Platform.runLater(() -> onStatusChanged.accept(status));
    }
}