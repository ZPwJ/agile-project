package io.github.student.agile.resource.dto;

import io.github.student.agile.resource.analytics.charts.SprintChartDataCompleteness;
import io.github.student.agile.resource.analytics.metrics.MetricUnit;

import java.util.List;

/** WF-3 burndown of one sprint. Field names and types are part of the API contract (plan section 2.1). */
public record BurndownDTO(Integer sprintId, MetricUnit unit,
                          SprintChartDataCompleteness dataCompleteness,
                          List<String> warnings, List<BurndownPointDTO> points) {
}
