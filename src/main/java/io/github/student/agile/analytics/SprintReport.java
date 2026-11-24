package io.github.student.agile.analytics;

import java.time.ZonedDateTime;

public record SprintReport(
        Integer sprintId,
        String sprintName,
        Integer projectId,
        ZonedDateTime startDate,
        ZonedDateTime endDate,
        int totalIssues,
        int completedIssues,
        int incompleteIssues,
        int velocity,
        int incompleteEstimation) {
}
