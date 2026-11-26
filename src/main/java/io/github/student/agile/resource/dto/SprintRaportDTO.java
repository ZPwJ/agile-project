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
public class SprintRaportDTO {

    private Integer sprintId;
    private String sprintName;
    private Integer projectId;
    private ZonedDateTime startTime;
    private Integer countIssues;
    private Integer countIssuesDone;
    private Integer countIssuesToDo;
    private Integer sumVelocityDone;
    private Integer sumVelocityToDo;
}
