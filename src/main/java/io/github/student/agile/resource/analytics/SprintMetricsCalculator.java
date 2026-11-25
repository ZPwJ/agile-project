package io.github.student.agile.resource.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.dto.analytics.SprintMetrics;

import java.util.List;

/**
 * Calculates the metrics of a single sprint (issue counts, velocity, estimation of uncompleted issues).
 *
 * <p>Implementations do not access the database: they receive the complete sprint data
 * (issues and chronologically sorted events), so the caller can load it in bulk
 * instead of running a separate query per issue.</p>
 */
public interface SprintMetricsCalculator {

    /**
     * @param sprint the sprint the metrics refer to
     * @param issues issues currently assigned to the sprint
     * @param events events of this sprint in chronological order
     * @return sprint metrics
     */
    SprintMetrics calculate(Sprint sprint, List<Issue> issues, List<SprintIssueEvent> events);
}
