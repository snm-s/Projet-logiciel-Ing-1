package controller.AdminPage;

import model.simulation.FloodSimulation;
import model.zone.Zone;
import view.SimulationView;

import javafx.application.Platform;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller principal de la simulation d'inondation.
 * Gère la logique métier entre SimulationInondation (modèle) et SimulationView (vue).
 */
public class SimulationController {

    private final FloodSimulation modele;
    private double vitesseSimulationMs;

    // Callbacks optionnels pour notifier la vue de changements d'état
    private Consumer<String> onStatusChanged;
    private Consumer<Double> onWaterLevelChanged;
    private Consumer<List<Zone>> onZonesUpdated;

    // Mode simulation : true = aléatoire, false = manuelle
    private boolean modeAleatoire = true;

    // ─────────────────────────────────────────────────────────────────────
    public SimulationController() {
        this.modele = new FloodSimulation();
        this.vitesseSimulationMs = 500.0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACCÈS AU MODÈLE
    // ─────────────────────────────────────────────────────────────────────

    public FloodSimulation getModele() {
        return modele;
    }

    public List<Zone> getZones() {
        return modele.getZones();
    }

    public Zone getZoneById(int id) {
        return modele.getZones().stream()
                .filter(z -> z.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Zone getZoneByName(String name) {
        return modele.getZones().stream()
                .filter(z -> z.getName() != null && z.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONTRÔLES SIMULATION
    // ─────────────────────────────────────────────────────────────────────

    /** Démarre ou reprend la simulation. */
    public void demarrerSimulation() {
        modele.demarrer();
        notifyStatus("Simulation en cours");
    }

    /** Met la simulation en pause. */
    public void mettreEnPause() {
        modele.setEnPause(true);
        notifyStatus("En pause");
    }

    /** Reprend après une pause. */
    public void reprendreSimulation() {
        modele.setEnPause(false);
        notifyStatus("Simulation en cours");
    }

    /**
     * Exécute un pas de simulation et notifie les listeners.
     * Appelé par le Timeline dans SimulationView.
     */
    public void executerPas() {
        if (modele.isEnPause()) return;

        modele.executerPas();

        // Notifier les changements
        Platform.runLater(() -> {
            if (onWaterLevelChanged != null) {
                onWaterLevelChanged.accept(modele.getNiveauEau());
            }
            if (onZonesUpdated != null) {
                onZonesUpdated.accept(modele.getZones());
            }
        });
    }

    /** Remet la simulation à zéro. */
    public void resetSimulation() {
        modele.resetSimulation();
        notifyStatus("Simulation réinitialisée");
        Platform.runLater(() -> {
            if (onZonesUpdated != null) onZonesUpdated.accept(modele.getZones());
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(0.0);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // PARAMÈTRES
    // ─────────────────────────────────────────────────────────────────────

    public void setVitesseSimulation(double vitesseSimulationMs) {
        this.vitesseSimulationMs = Math.max(100.0, vitesseSimulationMs);
    }

    public double getVitesseSimulationMs() {
        return vitesseSimulationMs;
    }

    /**
     * Définit la gravité/vitesse de montée des eaux.
     * Correspond au slider vitesse dans la vue.
     */
    public void setGravite(double gravite) {
        modele.setGravite(gravite);
    }

    /**
     * Force un niveau d'eau précis (simulation manuelle).
     */
    public void setNiveauEau(double niveauEau) {
        modele.setNiveauEau(niveauEau);
        Platform.runLater(() -> {
            if (onWaterLevelChanged != null) onWaterLevelChanged.accept(niveauEau);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // MODE SIMULATION
    // ─────────────────────────────────────────────────────────────────────

    public void setModeAleatoire(boolean aleatoire) {
        this.modeAleatoire = aleatoire;
        // Informer le modèle si besoin
        if (modele instanceof FloodSimulation) {
            // À étendre si FloodSimulation expose un setMode(...)
        }
    }

    public boolean isModeAleatoire() {
        return modeAleatoire;
    }

    // ─────────────────────────────────────────────────────────────────────
    // STATISTIQUES (utilisées par SimulationView.actualiserUI)
    // ─────────────────────────────────────────────────────────────────────

    public int getPopulationARisque() {
        return modele.getZones().stream()
                .filter(Zone::isFlooded)
                .mapToInt(Zone::getPopulation)
                .sum();
    }

    public int getPopulationEnSecurite() {
        return modele.getZones().stream()
                .filter(z -> !z.isFlooded())
                .mapToInt(Zone::getPopulation)
                .sum();
    }

    public long getNombreZonesInondees() {
        return modele.getZones().stream().filter(Zone::isFlooded).count();
    }

    public long getNombreZonesSures() {
        return modele.getZones().stream().filter(z -> !z.isFlooded()).count();
    }

    /**
     * Pourcentage d'arêtes dans chaque état (sûre, risque, inondée).
     * Retourne un tableau [pourcentSure, pourcentRisque, pourcentInondee].
     */
    public double[] getStatutReseau() {
        double nv = modele.getNiveauEau();
        // Logique basée sur le niveau d'eau global
        if (nv <= 0) return new double[]{100.0, 0.0, 0.0};
        double inondee = Math.min(nv / 3.0 * 100.0, 60.0);
        double risque  = Math.min(nv / 2.0 * 30.0, 30.0);
        double sure    = Math.max(100.0 - inondee - risque, 0.0);
        return new double[]{sure, risque, inondee};
    }

    // ─────────────────────────────────────────────────────────────────────
    // CALLBACKS (optionnels, utilisés si la vue veut s'abonner)
    // ─────────────────────────────────────────────────────────────────────

    public void setOnStatusChanged(Consumer<String> callback) {
        this.onStatusChanged = callback;
    }

    public void setOnWaterLevelChanged(Consumer<Double> callback) {
        this.onWaterLevelChanged = callback;
    }

    public void setOnZonesUpdated(Consumer<List<Zone>> callback) {
        this.onZonesUpdated = callback;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVÉ
    // ─────────────────────────────────────────────────────────────────────

    private void notifyStatus(String status) {
        if (onStatusChanged != null) {
            Platform.runLater(() -> onStatusChanged.accept(status));
        }
    }
}
