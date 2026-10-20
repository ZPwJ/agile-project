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
    private Boolean active;
    private String sprintGoal;
    private Integer projectID;
}
