package io.github.student.agile.analytics;

/**
 * Basic metrics of a single sprint, produced by {@link SprintMetricsCalculator}.
 *
 * <p>Estimation sums are in story points. An issue without estimation is counted
 * in the issue counters but adds nothing to the point sums.
 *
 * @param totalIssues          issues assigned to the sprint
 * @param completedIssues      issues with status DONE
 * @param incompleteIssues     issues with any other status
 * @param completedEstimation  sum of estimations of DONE issues
 * @param incompleteEstimation sum of estimations of not completed issues
 */
public record SprintMetrics(
        int totalIssues,
        int completedIssues,
        int incompleteIssues,
        int completedEstimation,
        int incompleteEstimation) {

    public static final SprintMetrics EMPTY = new SprintMetrics(0, 0, 0, 0, 0);

    /**
     * Sprint velocity (WF-2): sum of estimations of DONE issues assigned to the sprint.
     */
    public int velocity() {
        return completedEstimation;
    }

    /** Sum of estimations of all sprint issues, i.e. the current sprint scope. */
    public int totalEstimation() {
        return completedEstimation + incompleteEstimation;
    }
}
