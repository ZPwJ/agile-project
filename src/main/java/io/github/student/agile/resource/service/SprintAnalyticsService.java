package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.BurnupChartDTO;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;

public interface SprintAnalyticsService {

    SprintSummaryReportDTO getSprintSummaryReport(Integer sprintId);

    BurnupChartDTO getBurnupChart(Integer sprintId);
}
