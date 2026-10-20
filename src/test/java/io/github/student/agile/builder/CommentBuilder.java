package io.github.student.agile.builder;

import io.github.student.agile.resource.dto.request.CommentMRO;
import org.apache.commons.lang3.RandomStringUtils;

public class CommentBuilder {

    public static final String DEFAULT_COMMENT_BODY = "Default comment body";
    public static final String ANOTHER_COMMENT_BODY = "Another comment body";
    public static final String RANDOM_COMMENT_BODY = RandomStringUtils.random(10);

    public static CommentMRO createDefaultCommentMRO(Integer issueID) {
        return CommentMRO.builder()
                .issueID(issueID)
                .body(DEFAULT_COMMENT_BODY)
                .build();
    }

    public static CommentMRO createCommentMROWithCommentID(Integer issueID, Integer commentID) {
        return CommentMRO.builder()
                .commentID(commentID)
                .issueID(issueID)
                .body(ANOTHER_COMMENT_BODY)
                .build();
    }
}
