package io.github.student.agile.resource.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserAssociationDTO {

    private String email;
    private Integer projectID;
}
