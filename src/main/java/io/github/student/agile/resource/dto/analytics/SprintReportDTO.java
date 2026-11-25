package io.github.student.agile.resource.dto.analytics;

import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * Summary report of a single sprint (WF-1).
 *
 * @param startDate actual sprint start (ISO-8601)
 * @param endDate   actual sprint end (ISO-8601), {@code null} for an active sprint
 * @param unit      estimation unit, e.g. {@code STORY_POINTS}
 */
@Builder
public record SprintReportDTO(
        Integer sprintId,
        String sprintName,
        Integer projectId,
        ZonedDateTime startDate,
        ZonedDateTime endDate,
        int initialIssueCount,
        int completedIssueCount,
        int uncompletedIssueCount,
        int velocity,
        int uncompletedEstimation,
        String unit
) {
}
