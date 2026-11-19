package io.github.student.agile.resource.analytics.metrics;

/**
 * What the calculator needs to know about one issue of a sprint.
 *
 * @param estimation story points, {@code null} when not estimated
 * @param completed  whether the issue counts as completed
 */
public record IssueOutcome(Integer estimation, boolean completed) {
}
