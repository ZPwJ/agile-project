package io.github.student.agile.resource.analytics.charts;

/**
 * One task of a sprint at chart granularity.
 *
 * @param issueId    the task id
 * @param estimation story points; {@code null} means "not estimated" and counts as 0
 * @param completed  completed in the scope source (snapshot for a finished sprint, live issue otherwise)
 */
public record SprintTaskPoints(Integer issueId, Integer estimation, boolean completed) {
}
