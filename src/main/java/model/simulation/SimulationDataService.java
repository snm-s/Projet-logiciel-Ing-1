/**
 * SimulationDataService
 *
 * This service manages the persistence layer of the simulation.
 *
 * Responsibilities:
 * - Load agents and zones from JSON files at startup.
 * - Save modifications immediately to disk.
 * - Maintain immutable snapshots of the initial state.
 * - Restore the original state through reset operations.
 * - Generate random agents for demonstrations and testing.
 *
 * Architectural role:
 * This class separates persistence concerns from the simulation engine.
 * FloodSimulation focuses on simulation logic, while SimulationDataService
 * handles all data access operations.
 *
 * Design principles illustrated:
 * - Separation of Concerns (SoC)
 * - Single Responsibility Principle (SRP)
 * - Defensive Programming
 * - Persistence Layer abstraction
 */