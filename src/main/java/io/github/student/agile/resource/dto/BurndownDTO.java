package io.github.student.agile.resource.dto;

import io.github.student.agile.resource.analytics.charts.SprintChartDataCompleteness;
import io.github.student.agile.resource.analytics.metrics.MetricUnit;

import java.util.List;

/**
 * WF-3 burndown of one sprint. Field names and types are part of the API contract
 * (docs/analytics/wf3-wf4-charts.md, Response fields).
 */
public record BurndownDTO(Integer sprintId, MetricUnit unit,
                          SprintChartDataCompleteness dataCompleteness,
                          List<String> warnings, List<BurndownPointDTO> points) {
}
