package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.BurnupChartDTO;
import io.github.student.agile.resource.dto.SprintComparisonItemDTO;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;

import java.util.List;

public interface SprintAnalyticsService {

    SprintSummaryReportDTO getSprintSummaryReport(Integer sprintId);

    BurnupChartDTO getBurnupChart(Integer sprintId);

    List<SprintComparisonItemDTO> getProjectSprintsComparison(Integer projectId);
}
