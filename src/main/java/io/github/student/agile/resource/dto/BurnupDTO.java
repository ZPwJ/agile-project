package io.github.student.agile.resource.dto;

import io.github.student.agile.resource.analytics.charts.SprintChartDataCompleteness;
import io.github.student.agile.resource.analytics.metrics.MetricUnit;

import java.util.List;

/** WF-4 burnup of one sprint. Field names and types are part of the API contract (plan section 2.2). */
public record BurnupDTO(Integer sprintId, MetricUnit unit,
                        SprintChartDataCompleteness dataCompleteness,
                        List<String> warnings, List<BurnupPointDTO> points) {
}
