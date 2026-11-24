package io.github.student.agile.analytics;

import java.time.ZonedDateTime;

public record SprintComparisonDTO(
        Integer projectId,
        Integer sprintId,
        String sprintName,
        ZonedDateTime startDate,
        ZonedDateTime endDate,
        SprintComparisonStatus status,
        int completedIssueCount,
        int incompleteIssueCount,
        int velocity,
        String estimationUnit) {
}