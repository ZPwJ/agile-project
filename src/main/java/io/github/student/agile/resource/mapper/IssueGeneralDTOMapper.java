package io.github.student.agile.resource.mapper;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.dto.IssueGeneralDTO;
import io.github.student.agile.resource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
public class IssueGeneralDTOMapper {

    private final UserService userService;

    public IssueGeneralDTO map(Issue from) {
        return IssueGeneralDTO.builder()
                .id(from.getId())
                .name(from.getName())
                .assignee(
                        isNull(from.getAssigneeID()) ? null : userService.getUserByUUID(from.getAssigneeID())
                )
                .issueStatusID(
                        isNull(from.getIssueStatus()) ? null : from.getIssueStatus().getId()
                )
                .issueTypeID(
                        isNull(from.getIssueType()) ? null : from.getIssueType().getId()
                )
                .issuePriorityID(
                        isNull(from.getIssuePriority()) ? null : from.getIssuePriority().getId()
                )
                .projectID(from.getProjectID())
                .sprintID(
                        isNull(from.getSprint()) ? null : from.getSprint().getId()
                )
                .build();
    }
}
