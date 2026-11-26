package io.github.student.agile.resource.mapper;


import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.dto.SprintRaportDTO;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class SprintRaportDTOMapper {
   public SprintRaportDTO map(
           Sprint sprintRaport,
           Integer countIssues,
           Integer countIssuesDone,
           Integer countIssuesNotDone,
           Integer sumVelocityIssuesDone,
           Integer sumVelocityIssuesNotDone
   ) {

//       private Integer sumVelocityNotDone;

       return SprintRaportDTO.builder()
               .sprintId(sprintRaport.getId())
               .sprintName(sprintRaport.getName())
               .startTime(sprintRaport.getStartDate())
               .projectId(sprintRaport.getProject().getId())
               .countIssues(countIssues)
               .countIssuesDone(countIssuesDone)
               .countIssuesNotDone(countIssuesNotDone)
               .sumVelocityDone(sumVelocityIssuesDone)
               .sumVelocityNotDone(sumVelocityIssuesNotDone)
               .build();
   }

}
