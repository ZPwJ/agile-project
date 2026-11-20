package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.BurndownChartDTO;
import io.github.student.agile.resource.dto.BurnupChartDTO;
import io.github.student.agile.resource.dto.SprintComparisonItemDTO;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;

import java.time.ZonedDateTime;
import java.util.List;

public interface SprintAnalyticsService {

    SprintSummaryReportDTO getSprintSummaryReport(Integer sprintId);

    BurndownChartDTO getBurndownChart(Integer sprintId);

    BurnupChartDTO getBurnupChart(Integer sprintId);

    List<SprintComparisonItemDTO> getProjectSprintsComparison(Integer projectId, ZonedDateTime from, ZonedDateTime to);

    default List<SprintComparisonItemDTO> getProjectSprintsComparison(Integer projectId) {
        return getProjectSprintsComparison(projectId, null, null);
    }

    byte[] exportProjectSprintsComparisonCsv(Integer projectId, ZonedDateTime from, ZonedDateTime to);

    default byte[] exportProjectSprintsComparisonCsv(Integer projectId) {
        return exportProjectSprintsComparisonCsv(projectId, null, null);
    }
}
