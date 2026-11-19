package io.github.student.agile.resource.service.analytics;

/**
 * Issue counts and estimation sums of one sprint, in story points.
 *
 * @param historyAvailable      false when the sprint has no recorded events and the metrics reflect its current issues
 * @param initialIssueCount     issues in the sprint when it started
 * @param completedIssueCount   issues in scope and completed
 * @param uncompletedIssueCount issues in scope and not completed
 * @param velocity              sum of estimations of completed issues, taken at the moment of completion
 * @param uncompletedEstimation sum of the latest estimations of uncompleted issues
 */
public record SprintMetrics(
        boolean historyAvailable,
        int initialIssueCount,
        int completedIssueCount,
        int uncompletedIssueCount,
        int velocity,
        int uncompletedEstimation) {
}
