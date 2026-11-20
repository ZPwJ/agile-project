package io.github.student.agile.resource.analytics.charts;

import io.github.student.agile.resource.analytics.metrics.MetricUnit;

import java.util.List;

/**
 * Chart data of one sprint: the inclusive day axis and the points finished by the end of each day.
 * Days are ascending and unique; warnings are already rendered as {@code "CODE: message"}
 * in a deterministic order.
 */
public record SprintTimeline(Integer sprintId, MetricUnit unit, long initialScope,
                             List<SprintTimelineDay> days,
                             SprintChartDataCompleteness dataCompleteness,
                             List<String> warnings) {

    public SprintTimeline {
        days = List.copyOf(days);
        warnings = List.copyOf(warnings);
    }
}
