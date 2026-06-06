package controller;

import model.agent.Agent;
import model.agent.Citizen;
import model.alert.Alert;
import model.graph.Route;
import model.simulation.FloodSimulation;

public class CitizenController {

    private final FloodSimulation simulation;

    public CitizenController(FloodSimulation simulation) {
        this.simulation = simulation;
    }

    // ── Identité ────────────────────────────────────────────────────────────────

    /** Prénom de l'utilisateur connecté, ou valeur par défaut. */
    public String getFirstName(Agent user) {
        return (user != null && user.getFirstName() != null) ? user.getFirstName() : "Citoyen";
    }

    /** Nom complet (prénom + nom) de l'utilisateur connecté. */
    public String getFullName(Agent user) {
        String firstName = getFirstName(user);
        String lastName  = (user != null && user.getLastName() != null) ? user.getLastName() : "";
        return lastName.isEmpty() ? firstName : firstName + " " + lastName;
    }

    // ── État ────────────────────────────────────────────────────────────────────

    /**
     * Retourne le nom de l'état du citoyen (ex. "CALME", "PANIQUE").
     *
     * @throws IllegalStateException si l'utilisateur n'est pas un Citizen.
     */
    public String getCitizenState(Agent user) {
        if (user instanceof Citizen c) {
            return (c.getState() != null) ? c.getState().name() : "CALME";
        }
        throw new IllegalStateException("L'utilisateur connecté n'est pas un citoyen !");
    }

    /** Vrai si l'état est PANIQUE ou FOLIE (utile pour colorer l'avatar). */
    public boolean isCitizenInPanic(Agent user) {
        String state = getCitizenState(user);
        return "PANIQUE".equalsIgnoreCase(state) || "FOLIE".equalsIgnoreCase(state);
    }

    // ── Position & navigation ───────────────────────────────────────────────────

    /** Libellé de la position actuelle de l'agent. */
    public String getPositionLabel(Agent user) {
        return (user != null && user.getPosition() != null)
                ? "Nœud #" + user.getId()
                : "Inconnue";
    }

    /** Libellé de la destination assignée, ou message par défaut. */
    public String getTargetRefugeLabel(Agent user) {
        return (user != null && user.getDestination() != null)
                ? "Nœud #" + user.getDestination().getId()
                : "Aucun refuge assigné";
    }

    /**
     * Distance simulée vers le refuge (km).
     * Retourne -1 si l'agent n'a pas de destination.
     */
    public double getEvacuationDistanceKm(Agent user) {
        if (user != null && user.getDestination() != null) {
            return 1.2; // TODO : brancher un calcul réel sur le graphe
        }
        return -1;
    }

    /**
     * Temps estimé d'arrivée en minutes.
     * Retourne -1 si impossible à calculer.
     */
    public int getEtaMinutes(Agent user) {
        double distKm = getEvacuationDistanceKm(user);
        if (distKm < 0) return -1;
        if (user.getMaxSpeed() > 0) {
            return (int) ((distKm / user.getMaxSpeed()) * 60);
        }
        return 18; // vitesse par défaut
    }

    /** Libellé affiché pour la distance (ex. "1.2 km" ou "Calcul..."). */
    public String getDistanceLabel(Agent user) {
        double d = getEvacuationDistanceKm(user);
        return (d >= 0) ? d + " km" : "Calcul...";
    }

    /** Libellé affiché pour l'ETA (ex. "18 min" ou "-- min"). */
    public String getEtaLabel(Agent user) {
        int eta = getEtaMinutes(user);
        return (eta >= 0) ? eta + " min" : "-- min";
    }

    /** Libellé de statut d'itinéraire. */
    public String getRouteStatusLabel(Agent user) {
        return (user != null && user.isSaved()) ? "Arrivé" : "En cours";
    }

    // ── Alertes ─────────────────────────────────────────────────────────────────

    public int getAlertCount() {
        return simulation.getAlertSystem().getActiveAlerts().size();
    }

    public Alert getLatestCriticalAlert() {
        return simulation.getAlertSystem().getLatestAlert();
    }

    // ── Itinéraire ──────────────────────────────────────────────────────────────

    /** Calcule une route d'évacuation (Dijkstra/A* à brancher). */
    public Route calculateEvacuationRoute(model.graph.Node citizenNode) {
        // TODO : brancher Dijkstra/AStar
        return new Route();
    }
}
