package io.github.student.agile.resource.domain.dictionary;

/**
 * Type of an event that changes the scope or progress of a sprint.
 * Events are recorded only for an active sprint and are used to reconstruct
 * its history for reports (velocity, burndown, burnup).
 */
public enum SprintEventType {

    /** The issue belonged to the sprint when it started. */
    SCOPE_AT_START,

    /** The issue was added to the sprint while it was active. */
    ADDED,

    /** The issue was removed from the sprint (unassigned or deleted) while it was active. */
    REMOVED,

    /** The estimation of an issue in the sprint changed. */
    ESTIMATION_CHANGED,

    /** The issue reached the final status DONE. */
    COMPLETED
}
