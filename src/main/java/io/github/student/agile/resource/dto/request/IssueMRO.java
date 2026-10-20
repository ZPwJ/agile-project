package io.github.student.agile.resource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IssueMRO {

    private Integer issueID;
    private String name;
    private String description;
    private Integer estimation;
    private Integer projectID;
    private Integer issueTypeID;
    private Integer issuePriorityID;
}
