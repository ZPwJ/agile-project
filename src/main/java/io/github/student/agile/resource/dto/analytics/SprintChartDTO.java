package io.github.student.agile.resource.dto.analytics;

import lombok.Builder;

import java.util.List;

/**
 * Response of the burndown and burnup endpoints.
 *
 * @param unit             unit of the point values, e.g. {@code STORY_POINTS}
 * @param historyAvailable {@code false} when the sprint has no recorded event history
 *                         ({@code points} is then empty instead of guessed data)
 * @param points           points in chronological order
 */
@Builder
public record SprintChartDTO(
        Integer sprintId,
        String unit,
        boolean historyAvailable,
        List<ChartPointDTO> points
) {
}
