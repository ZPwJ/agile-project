package io.github.student.agile.resource.mapper;

import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.dto.ProjectDTO;
import org.springframework.stereotype.Component;

@Component
public class ProjectDTOMapper {

    public ProjectDTO map(Project from) {
        return ProjectDTO.builder()
                .projectID(from.getId())
                .name(from.getName())
                .description(from.getDescription())
                .projectGoal(from.getProjectGoal())
                .build();
    }
}
