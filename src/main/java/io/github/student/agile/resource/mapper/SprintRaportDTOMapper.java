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
           Integer countIssuesDone
   ) {

//       private Integer countIssuesDone;
//       private Integer countIssuesToDo;
//       private Integer sumVelocityDone;
//       private Integer sumVelocityToDo;

       return SprintRaportDTO.builder()
               .sprintId(sprintRaport.getId())
               .sprintName(sprintRaport.getName())
               .startTime(sprintRaport.getStartDate())
               .projectId(sprintRaport.getProject().getId())
               .countIssues(countIssues)
               .build();
   }

}
