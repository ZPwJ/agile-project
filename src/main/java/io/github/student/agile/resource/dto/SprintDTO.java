package io.github.student.agile.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SprintDTO {

    private Integer id;
    private String name;
    private ZonedDateTime startTime;
    /** Read-only: actual sprint start, ignored on create and update. */
    private ZonedDateTime startedAt;
    /** Read-only: actual sprint end, ignored on create and update. */
    private ZonedDateTime endDate;
    private Boolean active;
    private String sprintGoal;
    private Integer projectID;
}
