
package io.github.student.agile.resource.dto;

import io.github.student.agile.resource.analytics.metrics.MetricUnit;
import io.github.student.agile.resource.analytics.metrics.SprintState;

import java.time.ZonedDateTime;

public record SprintComparisonRowDTO(
        Integer sprintId,
        String sprintName,
        Integer projectId,
        ZonedDateTime startDate,
        ZonedDateTime endDate,
        SprintState state,
        int completedTasks,
        int incompleteTasks,
        long velocity,
        MetricUnit unit
) {
}
