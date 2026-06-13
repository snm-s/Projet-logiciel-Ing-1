/**
 * FloodSimulation
 *
 * Core engine of the flood evacuation simulation.
 *
 * Responsibilities:
 * - Manage the simulation lifecycle (start, pause, reset, step execution).
 * - Simulate flood propagation over time.
 * - Coordinate evacuation processes.
 * - Manage agents and zones.
 * - Generate alerts and evacuation events.
 * - Notify observers and UI components of changes.
 * - Maintain the history of evacuation actions.
 *
 * Architectural role:
 * This class acts as the central coordinator of the application.
 * It follows the role of a simulation engine and serves as the main
 * source of truth for the current state of the disaster scenario.
 *
 * Software engineering principles demonstrated:
 * - Observer Pattern (notifications to UI and listeners)
 * - Event-driven architecture
 * - Separation of concerns
 * - Defensive programming
 * - Encapsulation of simulation logic
 */