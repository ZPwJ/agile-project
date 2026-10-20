package io.github.student.agile.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueDTO {

    private Integer issueID;
    private String name;
    private String description;
    private Integer estimation;
    private UserDTO reporter;
    private UserDTO assignee;
    private Integer issueTypeID;
    private Integer issuePriorityID;
    private Integer issueStatusID;
    private Integer projectID;
    private Integer sprintID;
    private List<CommentDTO> comments;
}
