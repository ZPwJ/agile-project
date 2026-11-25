package io.github.student.agile.resource.analytics;

import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.dto.analytics.ChartPointDTO;

import java.util.List;

/**
 * Calculates burndown and burnup chart data points from the sprint event history.
 *
 * <p>A single point holds the remaining work, the cumulative completed work and the total scope,
 * so the same result serves both charts.</p>
 */
public interface SprintChartCalculator {

    /**
     * @param sprint the sprint the chart refers to
     * @param events events of this sprint in chronological order
     * @return points in chronological order; an empty list when there is no event history
     */
    List<ChartPointDTO> calculatePoints(Sprint sprint, List<SprintIssueEvent> events);
}
