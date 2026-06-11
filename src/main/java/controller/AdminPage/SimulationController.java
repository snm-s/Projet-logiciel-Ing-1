package controller.AdminPage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import app.Main;
import controller.MapController;
import javafx.application.Platform;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.RescueAgent;
import model.graph.AgentMovement;
import model.graph.EdgeState;
import model.simulation.FloodSimulation;
import model.simulation.SimulationDataService;
import model.zone.Zone;

/**
 * Contrôleur principal de la simulation d'inondation.
 *
 * <h3>Responsabilités</h3>
 * <ul>
 *   <li>Orchestre {@link FloodSimulation} (modèle) et {@link MapController} (vue).</li>
 *   <li>Délègue la persistance JSON à {@link SimulationDataService} :
 *       tout ajout/suppression d'agent ou de zone est immédiatement sauvegardé.</li>
 *   <li>Le {@link #resetSimulation()} recharge l'état initial depuis le snapshot JSON.</li>
 *   <li>Propage les modifications de carte vers {@link MapController} et le modèle
 *       afin que {@link Citizen} et {@link RescueAgent} opèrent toujours sur les
 *       données courantes.</li>
 * </ul>
 */
public class SimulationController {

    // ─── Modèle & services ────────────────────────────────────────────────
    private final FloodSimulation        modele;
    private final SimulationDataService  dataService;

    // ─── MapController (injecté après construction) ───────────────────────
    private MapController mapController;

    // ─── Paramètres ───────────────────────────────────────────────────────
    private double  vitesseSimulationMs = 500.0;
    private boolean modeAleatoire       = true;

    /** Seuil de niveau d'eau (m) déclenchant l'évacuation automatique. */
    private static final double SEUIL_EVACUATION_M = 0.5;

    /** Durée d'un pas simulé en secondes (RouteGraph.tick). */
    private static final double DELTA_SECONDS = 1.0;

    // ─── État interne ─────────────────────────────────────────────────────
    private boolean evacuationDeclenchee = false;

    // ─── Callbacks UI ─────────────────────────────────────────────────────
    private Consumer<String>        onStatusChanged;
    private Consumer<Double>        onWaterLevelChanged;
    private Consumer<List<Zone>>    onZonesUpdated;
    private Consumer<List<Agent>>   onAgentsUpdated;
    private Consumer<AgentMovement> onAgentArrived;

    // ─────────────────────────────────────────────────────────────────────

    /**
     * Constructeur principal — reçoit les instances partagées depuis Main.
     */
    public SimulationController(FloodSimulation simulation, SimulationDataService dataService) {
        this.modele      = simulation;
        this.dataService = dataService;
        // Le snapshot est déjà pris par FloodSimulation au démarrage
        // On le prend ici seulement si ce n'est pas encore fait
        dataService.takeSnapshot(modele.getZones(), modele.getAgents());
    }

    /**
     * Constructeur de compatibilité — délègue vers l'instance partagée de Main.
     * À utiliser uniquement si SimulationController est instancié hors de Main.
     */
    public SimulationController() {
        this(app.Main.getSharedSimulation(), app.Main.getSharedDataService());
    }

    // ─────────────────────────────────────────────────────────────────────
    // INJECTION DU MAP CONTROLLER
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Injecte le {@link MapController} après sa création (évite la dépendance circulaire).
     * Branche le callback d'arrivée d'agents.
     */
    public void setMapController(MapController mc) {
        this.mapController = mc;
        mc.setOnAgentArrived(mv -> {
            if (onAgentArrived != null)
                Platform.runLater(() -> onAgentArrived.accept(mv));
        });
        // Synchroniser la liste d'agents du MapController avec le modèle courant
        mc.syncAgents(modele.getAgents());
    }

    public void setAgents(List<Agent> agents) {
        modele.setAgents(agents);

        if (mapController != null) {
            mapController.syncAgents(modele.getAgents());
        }
        notifyAgentsUpdated();
    }

    

    public void replaceAgents(List<Agent> agents) {
        modele.getAgents().clear();
        modele.getAgents().addAll(agents);

        if (mapController != null) {
            mapController.syncAgents(modele.getAgents());
        }

        notifyAgentsUpdated();
    }


    // ─────────────────────────────────────────────────────────────────────
    // ACCÈS AU MODÈLE
    // ─────────────────────────────────────────────────────────────────────

    public FloodSimulation  getModele()  { return modele; }
    public List<Zone>       getZones()   { return modele.getZones(); }
    public SimulationDataService getDataService() { return dataService; }

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
        modele.publishSimulationStartAlert();

        if (!evacuationDeclenchee) {
            declencherEvacuationAutomatique();
            evacuationDeclenchee = true;
        }

        notifyStatus("Simulation en cours — alerte envoyée aux citoyens");
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
     *   <li>Avance le modèle d'inondation.</li>
     *   <li>Vérifie le seuil d'évacuation automatique.</li>
     *   <li>Avance tous les agents sur la carte (tick MapController).</li>
     *   <li>Notifie les listeners UI.</li>
     * </ol>
     */
    public void executerPas() {
        if (modele.isEnPause()) return;

        // En mode aléatoire, l'eau monte automatiquement.
        // En mode manuel, on garde le niveau d'eau tel quel, mais on continue
        // à faire avancer les agents sur leurs trajets.
        if (modeAleatoire) {
            modele.executerPas();
        }

        double niveau = modele.getNiveauEau();

        if (!evacuationDeclenchee && niveau >= SEUIL_EVACUATION_M) {
            declencherEvacuationAutomatique();
            evacuationDeclenchee = true;
        }

        if (mapController != null) {
            mapController.tick(DELTA_SECONDS);
            mapController.updateAllZones(modele.getZones());
        }

        Platform.runLater(() -> {
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(niveau);
            if (onZonesUpdated      != null) onZonesUpdated.accept(modele.getZones());
        });
    }

    /**
     * Remet la simulation à l'état initial :
     * <ol>
     *   <li>Recharge zones et agents depuis le snapshot JSON.</li>
     *   <li>Réinitialise le modèle d'inondation.</li>
     *   <li>Synchronise MapController et MapView.</li>
     *   <li>Notifie l'UI.</li>
     * </ol>
     */
    public void resetSimulation() {
        // 1. Recharger depuis le snapshot
        Object[] initial = dataService.reset();
        @SuppressWarnings("unchecked")
        List<Zone>  zones  = (List<Zone>)  initial[0];
        @SuppressWarnings("unchecked")
        List<Agent> agents = (List<Agent>) initial[1];

        // 2. Réinitialiser le modèle
        modele.resetSimulation();
        if (!zones.isEmpty())  modele.setZones(zones);
        if (!agents.isEmpty()) modele.setAgents(agents);

        evacuationDeclenchee = false;

        // 3. Synchroniser MapController
        if (mapController != null) {
            mapController.syncAgents(modele.getAgents());
            mapController.syncZones(modele.getZones());
        }

        notifyStatus("Simulation réinitialisée");

        Platform.runLater(() -> {
            if (onZonesUpdated      != null) onZonesUpdated.accept(modele.getZones());
            if (onAgentsUpdated     != null) onAgentsUpdated.accept(modele.getAgents());
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(0.0);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // GESTION DES AGENTS (persistance immédiate)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Ajoute un citoyen aléatoire ancré sur une zone réelle, sauvegarde dans le JSON.
     */
    public Citizen addRandomCitizen() {
        int id = nextAgentId();
        Citizen c = dataService.createRandomCitizen(id, modele.getZones());
        modele.getAgents().add(c);
        persistAgents();
        propagateAgentAddition(c);
        return c;
    }

    /**
     * Ajoute {@code count} citoyens aléatoires en une seule opération JSON.
     */
    public List<Citizen> addRandomCitizens(int count) {
        List<Citizen> added = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int id = nextAgentId();
            Citizen c = dataService.createRandomCitizen(id, modele.getZones());
            modele.getAgents().add(c);
            added.add(c);
        }
        persistAgents();
        added.forEach(this::propagateAgentAddition);
        return added;
    }

    /**
     * Ajoute un agent de secours aléatoire.
     */
    public RescueAgent addRandomRescueAgent() {
        int id = nextAgentId();
        RescueAgent ra = dataService.createRandomRescueAgent(id, modele.getZones());
        modele.getAgents().add(ra);
        persistAgents();
        propagateAgentAddition(ra);
        return ra;
    }

    /**
     * Supprime un agent du modèle et du JSON.
     */
    public boolean removeAgent(int agentId) {
        boolean removed = modele.getAgents().removeIf(a -> a.getId() == agentId);
        if (removed) {
            persistAgents();
            if (mapController != null) mapController.removeAgent(agentId);
            notifyAgentsUpdated();
        }
        return removed;
    }

    // ─────────────────────────────────────────────────────────────────────
    // GESTION DES ZONES (persistance immédiate)
    // ─────────────────────────────────────────────────────────────────────

    public void updateZone(Zone zone) {
        modele.updateZone(zone);                          // modifie la source de vérité
        dataService.saveZones(modele.getZones());         // persistance optionnelle
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> {
            if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones());
        });
    }

    public void addZone(Zone zone) {
        modele.addZone(zone);                             // modifie la source de vérité
        dataService.saveZones(modele.getZones());         // persistance optionnelle
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> {
            if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones());
        });
    }

    public void removeZone(int zoneId) {
        modele.removeZoneById(zoneId);                    // modifie la source de vérité
        dataService.saveZones(modele.getZones());         // persistance optionnelle
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> {
            if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones());
        });
    }

    public Zone addRandomNeighborhood() {
        int id = modele.nextZoneId();
        java.util.Random rng = new java.util.Random();
        double lat = 45.7640 + (rng.nextDouble() - 0.5) * 0.06;
        double lng = 4.8357  + (rng.nextDouble() - 0.5) * 0.08;
        double alt = 0.5 + rng.nextDouble() * 3.5;
        int    pop = 50  + rng.nextInt(950);

        // Neighborhood est la sous-classe concrète standard (non-refuge)
        Zone z = new model.zone.Neighborhood(id, "Zone-" + id, lat, lng, alt, pop, "Générée automatiquement");

        modele.addZone(z);
        dataService.saveZones(modele.getZones());
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> {
            if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones());
        });
        return z;
    }

    public Zone addRandomShelter() {
        int id = modele.nextZoneId();
        java.util.Random rng = new java.util.Random();
        double lat = 45.7640 + (rng.nextDouble() - 0.5) * 0.06;
        double lng = 4.8357  + (rng.nextDouble() - 0.5) * 0.08;
        double alt = 2.0 + rng.nextDouble() * 4.0;   // refuges plus hauts
        int    pop = 0;
        int    cap = 100 + rng.nextInt(400);           // capacité 100 → 500

        Zone z = new model.zone.Shelter(id, "Refuge-" + id, lat, lng, alt, pop, "Refuge généré", cap);

        modele.addZone(z);
        dataService.saveZones(modele.getZones());
        if (mapController != null) mapController.syncZones(modele.getZones());
        Platform.runLater(() -> {
            if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones());
        });
        return z;
    }



    // ─────────────────────────────────────────────────────────────────────
    // STATISTIQUES
    // ─────────────────────────────────────────────────────────────────────

    public MapController getMapController() { return mapController; }

    public int getPopulationARisque() {
        if (mapController != null) return (int) mapController.countCitizensAtRisk();
        return (int) modele.getAgents().stream().filter(a -> a instanceof Citizen).count();
    }

    public int getPopulationEnSecurite() {
        if (mapController != null) return (int) mapController.countCitizensSafe();
        return 0;
    }

    public long getNombreZonesInondees() {
        return modele.getZones().stream().filter(Zone::isFlooded).count();
    }

    public long getNombreZonesSures() {
        return modele.getZones().stream().filter(z -> !z.isFlooded()).count();
    }

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
        var edges = mapController.getRouteGraph().getEdges();
        if (edges.isEmpty()) return new double[]{100.0, 0.0, 0.0, 0.0, 0.0};
        double total    = edges.size();
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
    // PARAMÈTRES
    // ─────────────────────────────────────────────────────────────────────

    public void setVitesseSimulation(double ms) { this.vitesseSimulationMs = Math.max(100.0, ms); }
    public double getVitesseSimulationMs()       { return vitesseSimulationMs; }
    public void setGravite(double g)             { modele.setGravite(g); }
    public void setNiveauEau(double nv) {
        modele.setNiveauEau(nv);
        Platform.runLater(() -> { if (onWaterLevelChanged != null) onWaterLevelChanged.accept(nv); });
    }
    public void setModeAleatoire(boolean b)      { this.modeAleatoire = b; }
    public boolean isModeAleatoire()             { return modeAleatoire; }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACKS
    // ─────────────────────────────────────────────────────────────────────

    public void setOnStatusChanged(Consumer<String>        cb) { this.onStatusChanged      = cb; }
    public void setOnWaterLevelChanged(Consumer<Double>    cb) { this.onWaterLevelChanged  = cb; }
    public void setOnZonesUpdated(Consumer<List<Zone>>     cb) { this.onZonesUpdated       = cb; }
    public void setOnAgentsUpdated(Consumer<List<Agent>>   cb) { this.onAgentsUpdated      = cb; }
    public void setOnAgentArrived(Consumer<AgentMovement>  cb) { this.onAgentArrived       = cb; }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVÉS
    // ─────────────────────────────────────────────────────────────────────

    private void declencherEvacuationAutomatique() {
        if (mapController == null) return;
        List<Citizen> citizens = modele.getAgents().stream()
            .filter(a -> a instanceof Citizen)
            .map(a -> (Citizen) a)
            .collect(Collectors.toList());
        mapController.triggerMassEvacuation(citizens);
        notifyStatus("⚠ Évacuation déclenchée : alertes envoyées et trajets attribués.");
    }

    private void propagateAgentAddition(Agent agent) {
        if (mapController != null) {
            mapController.addAgent(agent);
            mapController.syncAgents(modele.getAgents());

            // Si l'alerte a déjà été lancée, tout nouveau citoyen doit aussi
            // recevoir un trajet d'évacuation vers un refuge.
            if (evacuationDeclenchee && agent instanceof Citizen c) {
                mapController.evacuateCitizen(c);
            }
        }
    
        Platform.runLater(() -> {
            if (Main.getSharedMapView() != null) {
                Main.getSharedMapView().setAgents(modele.getAgents());
            }
        });
    
        notifyAgentsUpdated();
    }

    private void persistAgents() { dataService.saveAgents(modele.getAgents()); }
    private void persistZones()  { dataService.saveZones(modele.getZones()); }

    private int nextAgentId() {
        return modele.getAgents().stream()
            .mapToInt(Agent::getId).max().orElse(100) + 1;
    }

    private void notifyStatus(String status) {
        if (onStatusChanged != null)
            Platform.runLater(() -> onStatusChanged.accept(status));
    }

    private void notifyAgentsUpdated() {
        if (onAgentsUpdated != null)
            Platform.runLater(() -> onAgentsUpdated.accept(modele.getAgents()));
    }
}
