package io.github.student.agile.resource.dto.analytics;

import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * A row of the project sprint comparison (WF-5); the same columns go to the CSV export (WF-7).
 *
 * @param startDate actual sprint start (ISO-8601)
 * @param endDate   actual sprint end (ISO-8601), {@code null} for an active sprint
 */
@Builder
public record SprintComparisonRowDTO(
        Integer projectId,
        Integer sprintId,
        String sprintName,
        ZonedDateTime startDate,
        ZonedDateTime endDate,
        int completedIssueCount,
        int uncompletedIssueCount,
        int velocity
) {
}
