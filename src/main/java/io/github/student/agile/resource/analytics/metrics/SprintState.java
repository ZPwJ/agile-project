package io.github.student.agile.resource.analytics.metrics;

/** Lifecycle state of a sprint, derived from its {@code active} and {@code isDone} flags. */
public enum SprintState {
    /** Created, not started yet. */
    PLANNED,
    /** Currently running. */
    ACTIVE,
    /** Stopped; its results are read from the stop-time snapshot. */
    FINISHED
}
