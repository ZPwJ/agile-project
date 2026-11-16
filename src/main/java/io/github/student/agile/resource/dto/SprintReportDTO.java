package io.github.student.agile.resource.dto;

import java.time.Instant;

public record SprintReportDTO(
        Integer sprintID,
        String name,
        Integer projectID,
        Instant startDate,
        Instant endDate,
        Integer initialIssueCount,
        int completedIssueCount,
        int incompleteIssueCount,
        long velocity,
        long incompletePoints) {
}
