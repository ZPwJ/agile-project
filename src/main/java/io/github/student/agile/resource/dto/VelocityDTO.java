package io.github.student.agile.resource.dto;

import io.github.student.agile.resource.analytics.metrics.MetricUnit;
import io.github.student.agile.resource.analytics.metrics.SprintState;
import lombok.Builder;
import lombok.Data;

/** WF-2: sum of estimations of completed tasks of the sprint. */
@Data
@Builder
public class VelocityDTO {

    private Integer sprintId;
    private SprintState state;
    private MetricUnit unit;
    private long velocity;
}
