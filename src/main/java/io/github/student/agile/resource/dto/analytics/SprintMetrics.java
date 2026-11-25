package io.github.student.agile.resource.dto.analytics;

import lombok.Builder;

/**
 * Result of the sprint metrics calculator. Estimations are expressed in story points;
 * an issue without an estimation counts towards issue counts but adds 0 points.
 *
 * @param initialIssueCount     number of issues in the sprint scope when it started
 * @param completedIssueCount   number of completed issues (status DONE)
 * @param uncompletedIssueCount number of uncompleted issues
 * @param velocity              sum of estimations of completed issues
 * @param uncompletedEstimation sum of estimations of uncompleted issues
 */
@Builder
public record SprintMetrics(
        int initialIssueCount,
        int completedIssueCount,
        int uncompletedIssueCount,
        int velocity,
        int uncompletedEstimation
) {
}
