package model.simulation;

/**
 * FloodZone
 *
 * This class represents a geographical area exposed to flooding
 * during the simulation.
 *
 * Responsibilities:
 * - Store the characteristics of a flood-prone zone.
 * - Track its flooding status.
 * - Monitor the evacuation progress of its population.
 * - Restore its initial state when the simulation is reset.
 *
 * Design rationale:
 * FloodZone is a simplified domain model used by the simulation engine.
 * Instead of tracking every individual resident, the class manages
 * populations at an aggregate level, making large-scale simulations
 * more efficient.
 *
 * This abstraction allows us to:
 * - simulate disaster evolution quickly,
 * - generate evacuation statistics,
 * - estimate the effectiveness of emergency responses.
 */
public class FloodZone {

    /**
     * Human-readable name of the zone.
     *
     * Example:
     * "City Center", "North District", etc.
     */
    private final String name;

    /**
     * Altitude of the zone.
     *
     * Flooding occurs when the water level reaches or exceeds this value.
     */
    private final double altitude;

    /**
     * Initial number of inhabitants before the simulation starts.
     *
     * This value never changes and is used when resetting the simulation.
     */
    private final int initialPopulation;

    /**
     * Number of inhabitants still present in the zone.
     *
     * This value decreases as evacuation operations occur.
     */
    private int remainingPopulation;

    /**
     * Indicates whether the zone is currently flooded.
     */
    private boolean flooded;

    /**
     * Indicates whether the evacuation process has been completed.
     *
     * A zone is considered fully evacuated when no residents remain.
     */
    private boolean evacuated;

    /**
     * Creates a new flood-prone zone.
     *
     * Defensive programming:
     * Negative population values are automatically converted to zero.
     *
     * @param name       Name of the zone.
     * @param altitude   Flood threshold altitude.
     * @param population Initial population of the zone.
     */
    public FloodZone(String name, double altitude, int population) {

        this.name = name;
        this.altitude = altitude;

        /*
         * Prevent invalid population values.
         */
        this.initialPopulation = Math.max(population, 0);

        /*
         * At the beginning of the simulation,
         * all inhabitants are still present.
         */
        this.remainingPopulation = this.initialPopulation;

        /*
         * Initial conditions:
         * the zone is safe and not evacuated.
         */
        this.flooded = false;
        this.evacuated = false;
    }

    /**
     * Returns the zone name.
     *
     * @return Zone name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the flooding altitude threshold.
     *
     * @return Altitude value.
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * Indicates whether the zone is currently flooded.
     *
     * @return true if flooded.
     */
    public boolean isFlooded() {
        return flooded;
    }

    /**
     * Updates the flooding state.
     *
     * @param flooded New flood status.
     */
    public void setFlooded(boolean flooded) {
        this.flooded = flooded;
    }

    /**
     * Indicates whether evacuation has been completed.
     *
     * @return true if the entire population has been evacuated.
     */
    public boolean isEvacuated() {
        return evacuated;
    }

    /**
     * Returns the number of inhabitants still present in the zone.
     *
     * @return Remaining population.
     */
    public int getRemainingPopulation() {
        return remainingPopulation;
    }

    /**
     * Simulates an evacuation operation.
     *
     * Evacuation conditions:
     * - The zone must be flooded.
     * - The zone must not already be fully evacuated.
     * - There must still be inhabitants remaining.
     *
     * Simplified evacuation model:
     * Approximately half of the remaining population is evacuated
     * during each evacuation cycle.
     *
     * The method guarantees that at least one person is evacuated
     * whenever evacuation is possible.
     *
     * Example:
     *
     * Initial population = 100
     * First evacuation  → 50 evacuated
     * Remaining          → 50
     *
     * Second evacuation → 25 evacuated
     * Remaining         → 25
     *
     * This progressive approach models the fact that evacuations
     * become increasingly difficult as conditions deteriorate.
     *
     * @return Number of people evacuated during this operation.
     */
    public int evacuate() {

        /*
         * Evacuation cannot proceed if:
         * - the zone is already evacuated,
         * - the zone is not flooded,
         * - no inhabitants remain.
         */
        if (evacuated || !flooded || remainingPopulation <= 0) {
            return 0;
        }

        /*
         * Simplified rescue strategy:
         * evacuate roughly half of the remaining population.
         *
         * Ensure that at least one resident is evacuated.
         */
        int evacuatedCount = Math.max(1, remainingPopulation / 2);

        /*
         * Prevent evacuating more people than remain.
         */
        evacuatedCount = Math.min(evacuatedCount, remainingPopulation);

        /*
         * Update the population count.
         */
        remainingPopulation -= evacuatedCount;

        /*
         * The zone becomes fully evacuated once
         * no inhabitants remain.
         */
        evacuated = remainingPopulation == 0;

        return evacuatedCount;
    }

    /**
     * Restores the zone to its initial state.
     *
     * Effects:
     * - Restores the original population.
     * - Removes flooding.
     * - Cancels evacuation status.
     *
     * This method is mainly used when restarting the simulation.
     */
    public void reset() {

        this.remainingPopulation = this.initialPopulation;

        this.flooded = false;

        this.evacuated = false;
    }
}