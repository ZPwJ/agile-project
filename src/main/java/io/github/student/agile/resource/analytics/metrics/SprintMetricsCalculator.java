package io.github.student.agile.resource.analytics.metrics;

import java.util.Collection;

/** Pure calculation of {@link SprintMetrics}; the only place where the counting rules live. */
public final class SprintMetricsCalculator {

    private SprintMetricsCalculator() {
    }

    /**
     * Counts every outcome in the task counts. Estimations add to the sums only when present
     * (a missing estimation adds 0 points), so unestimated tasks never cause an error.
     */
    public static SprintMetrics calculate(Integer sprintId, SprintState state, Collection<IssueOutcome> outcomes) {
        int completed = 0;
        int incomplete = 0;
        int unestimated = 0;
        long completedEstimate = 0;
        long incompleteEstimate = 0;

        for (IssueOutcome outcome : outcomes) {
            int points = outcome.estimation() == null ? 0 : outcome.estimation();
            if (outcome.estimation() == null) {
                unestimated++;
            }
            if (outcome.completed()) {
                completed++;
                completedEstimate += points;
            } else {
                incomplete++;
                incompleteEstimate += points;
            }
        }

        return new SprintMetrics(sprintId, state, outcomes.size(), completed, incomplete, unestimated,
                completedEstimate, incompleteEstimate, MetricUnit.STORY_POINTS);
    }
}
