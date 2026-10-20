package io.github.student.agile.resource.mapper;

import io.github.student.agile.resource.domain.Comment;
import io.github.student.agile.resource.dto.CommentDTO;
import io.github.student.agile.resource.dto.UserDTO;
import io.github.student.agile.resource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentDTOMapper {

    private final UserService userService;

    public CommentDTO map(Comment from, UserDTO userDTO) {
        return CommentDTO.builder()
                .commentID(from.getId())
                .body(from.getBody())
                .author(userDTO)
                .build();
    }

    public CommentDTO mapV2(Comment from) {
        return CommentDTO.builder()
                .commentID(from.getId())
                .body(from.getBody())
                .author(userService.getUserByUUID(from.getCreatedBy()))
                .build();
    }
}
