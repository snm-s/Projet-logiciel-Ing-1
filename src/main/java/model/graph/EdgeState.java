package model.graph;

import java.awt.Color;

/**
 * État d'un edge (route inter-zones) enrichi avec la logique de capacité.
 *
 * Priorité d'affichage (du plus critique au moins critique) :
 *   FLOODED > OVERLOADED > CONGESTED > WARNING > NORMAL > SAFE
 */
public enum EdgeState {

    /** Route submergée (une ou deux zones inondées) — inaccessible. */
    FLOODED   (new Color(0x1A237E), true,  0),   // bleu nuit, tirets

    /** Capacité dépassée (flux > capacité max). */
    OVERLOADED(new Color(0xB71C1C), false, 0),   // rouge profond

    /** Congestion (flux > 80 % de la capacité). */
    CONGESTED (new Color(0xE53935), false, 0),   // rouge vif

    /** Attention (flux > 50 % — flux notable). */
    WARNING   (new Color(0xFB8C00), false, 0),   // orange

    /** Route praticable, flux faible. */
    NORMAL    (new Color(0xFDD835), false, 0),   // jaune

    /** Route libre, zones saines. */
    SAFE      (new Color(0x2E7D32), false, 0);   // vert

    // ─────────────────────────────────────────────────────────────────────
    public final Color   color;
    public final boolean dashed;
    /** Vitesse (km/h) indicative pour l'UI — 0 = bloqué. */
    public final int     speedKmh;

    EdgeState(Color color, boolean dashed, int speedKmh) {
        this.color    = color;
        this.dashed   = dashed;
        this.speedKmh = speedKmh;
    }

    // ─────────────────────────────────────────────────────────────────────
    // FACTORY
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Calcule l'état d'un edge en combinant l'état d'inondation des zones
     * et le ratio flux/capacité.
     *
     * @param fromFlooded zone source inondée ?
     * @param toFlooded   zone destination inondée ?
     * @param flowRatio   flux courant / capacité max  (0.0 – ∞)
     */
    public static EdgeState compute(boolean fromFlooded, boolean toFlooded,
                                    double flowRatio) {
        if (fromFlooded || toFlooded) return FLOODED;
        if (flowRatio > 1.0)          return OVERLOADED;
        if (flowRatio > 0.8)          return CONGESTED;
        if (flowRatio > 0.5)          return WARNING;
        if (flowRatio > 0.0)          return NORMAL;
        return SAFE;
    }

    /**
     * Surcharge sans flux (rétrocompatibilité).
     */
    public static EdgeState compute(boolean fromFlooded, boolean toFlooded,
                                    double altFrom, double altTo) {
        return compute(fromFlooded, toFlooded, 0.0);
    }
}