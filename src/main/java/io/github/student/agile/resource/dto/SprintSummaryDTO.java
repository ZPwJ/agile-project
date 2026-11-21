package io.github.student.agile.resource.dto;

import io.github.student.agile.resource.analytics.metrics.MetricUnit;
import io.github.student.agile.resource.analytics.metrics.SprintState;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

/** WF-1 sprint report. Dates are ISO-8601 with offset; {@code velocity} is the sum of completed estimations. */
@Data
@Builder
public class SprintSummaryDTO {

    private Integer sprintId;
    private String sprintName;
    private Integer projectId;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private SprintState state;
    private MetricUnit unit;
    private int totalTasks;
    private int completedTasks;
    private int incompleteTasks;
    private int unestimatedTasks;
    private long velocity;
    private long incompleteEstimate;
}
