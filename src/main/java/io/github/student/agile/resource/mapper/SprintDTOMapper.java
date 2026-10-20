package io.github.student.agile.resource.mapper;

import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.SprintDTO;
import org.springframework.stereotype.Component;

@Component
public class SprintDTOMapper {

    public SprintDTO map(Sprint from) {
        return SprintDTO.builder()
                .id(from.getId())
                .name(from.getName())
                .startTime(from.getStartDate())
                .sprintGoal(from.getSprintGoal())
                .active(from.getActive())
                .projectID(from.getProject().getId())
                .build();
    }
}
