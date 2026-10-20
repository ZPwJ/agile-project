package io.github.student.agile.resource.mapper;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.dto.IssueDTO;
import io.github.student.agile.resource.service.CommentService;
import io.github.student.agile.resource.service.UserService;
import io.github.student.agile.utils.collection.CollectionsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
public class IssueDTOMapper {

    private final UserService userService;
    private final CommentService commentService;

    public IssueDTO map(Issue from) {
        return IssueDTO.builder()
                .issueID(from.getId())
                .name(from.getName())
                .description(from.getDescription())
                .estimation(from.getEstimation())
                .reporter(userService.getUserSafely(from.getReporterID()))
                .assignee(userService.getUserSafely(from.getAssigneeID()))
                .projectID(from.getProjectID())
                .sprintID(
                        isNull(from.getSprint()) ? null : from.getSprint().getId()
                )
                .issueTypeID(
                        isNull(from.getIssueType()) ? null : from.getIssueType().getId()
                )
                .issuePriorityID(
                        isNull(from.getIssuePriority()) ? null : from.getIssuePriority().getId()
                )
                .issueStatusID(
                        isNull(from.getIssueStatus()) ? null : from.getIssueStatus().getId()
                )
                .comments(
                        CollectionsUtils.isNullOrEmpty(from.getComments()) ? new ArrayList<>() : commentService.getAllCommentsForIssueID(from.getId())
                )
                .build();
    }
}
