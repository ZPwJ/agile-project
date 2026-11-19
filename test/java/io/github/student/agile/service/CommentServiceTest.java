package io.github.student.agile.service;

import io.github.student.agile.builder.IssueBuilder;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Comment;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.dto.CommentDTO;
import io.github.student.agile.resource.dto.request.CommentMRO;
import io.github.student.agile.resource.repository.CommentRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.service.CommentService;
import io.github.student.agile.resource.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static io.github.student.agile.builder.CommentBuilder.RANDOM_COMMENT_BODY;
import static io.github.student.agile.builder.CommentBuilder.createCommentMROWithCommentID;
import static io.github.student.agile.builder.CommentBuilder.createDefaultCommentMRO;
import static io.github.student.agile.builder.UserBuilder.CURRENT_USER_UUID;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.builder.UserBuilder.prepareAnotherUserMock;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.COMMENT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.FORBIDDEN_COMMENT_ACCESS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_DOES_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class CommentServiceTest {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @MockBean
    private UserService userService;

    private Issue issueEntity;

    @BeforeEach
    void setUp() {
        when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        issueEntity = IssueBuilder.createIssueEntity();
        issueRepository.save(issueEntity);
    }

    @Test
    public void getAllCommentsForIssueID_happyPathTest() {

        //given
        when(userService.getUserByUUID(any())).thenReturn(createCurrentLoggedInUser());
        Integer commentID = createDefaultCommentForIssue();
        int expectedListSize = 1;

        //when
        List<CommentDTO> returnedCommentsList = commentService.getAllCommentsForIssueID(issueEntity.getId());
        int actualListSize = returnedCommentsList.size();
        CommentDTO commentDTO = returnedCommentsList.get(0);

        //then
        assertEquals(expectedListSize, actualListSize);
        assertEquals(commentID, commentDTO.getCommentID());
        assertEquals(RANDOM_COMMENT_BODY, commentDTO.getBody());
        assertEquals(createCurrentLoggedInUser(), commentDTO.getAuthor());
    }

    @Test
    public void getAllCommentsForIssueID_throwsIssueDoesNotExistsExceptionTest() {

        //given
        Integer fakeIssueID = issueEntity.getId() + 1;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> commentService.getAllCommentsForIssueID(fakeIssueID));

        //then
        assertEquals(ISSUE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void addCommentToIssue_happyPathTest() {

        //given
        CommentMRO mro = createDefaultCommentMRO(issueEntity.getId());
        long expectedCommentsAmount = commentRepository.count() + 1;

        //when
        CommentDTO createdComment = commentService.addCommentToIssue(mro);
        long actualCommentsAmount = commentRepository.count();

        //then
        assertEquals(expectedCommentsAmount, actualCommentsAmount);
        assertEquals(mro.getBody(), createdComment.getBody());
        assertEquals(userService.getCurrentLoggedUser(), createdComment.getAuthor());
    }

    @Test
    public void addCommentToIssue_throwsIssueDoesNotExistsExceptionTest() {

        //given
        Integer fakeIssueID = issueEntity.getId() + 1;
        CommentMRO mro = createDefaultCommentMRO(fakeIssueID);

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> commentService.addCommentToIssue(mro));

        //then
        assertEquals(ISSUE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void updateComment_happyPathTest() {

        //given
        Integer commentID = createDefaultCommentForIssue();
        CommentMRO mro = createCommentMROWithCommentID(issueEntity.getId(), commentID);

        //when
        CommentDTO updatedComment = commentService.updateComment(mro);

        //then
        assertEquals(commentID, updatedComment.getCommentID());
        assertEquals(mro.getBody(), updatedComment.getBody());
        assertEquals(createCurrentLoggedInUser(), updatedComment.getAuthor());
    }

    @Test
    public void updateComment_throwsCommentDoesNotExistsExceptionTest() {

        //given
        Integer fakeCommentID = -10;
        CommentMRO mro = createCommentMROWithCommentID(issueEntity.getId(), fakeCommentID);

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> commentService.updateComment(mro));

        //then
        assertEquals(COMMENT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void updateComment_throwsForbiddenCommentAccessExceptionTest() {

        //given
        Integer commentID = createDefaultCommentForIssue();
        when(userService.getCurrentLoggedUser()).thenReturn(prepareAnotherUserMock());
        CommentMRO mro = createCommentMROWithCommentID(issueEntity.getId(), commentID);

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> commentService.updateComment(mro));

        //then
        assertEquals(FORBIDDEN_COMMENT_ACCESS, exception.getMessage());
    }

    @Test
    public void deleteComment_happyPathTest() {

        //given
        Integer commentID = createDefaultCommentForIssue();
        long expectedCommentsAmount = commentRepository.count() - 1;

        //when
        commentService.deleteComment(commentID);
        long actualCommentsAmount = commentRepository.count();

        //then
        assertEquals(expectedCommentsAmount, actualCommentsAmount);
    }

    @Test
    public void deleteComment_throwsCommentDoesNotExistsExceptionTest() {

        //given
        Integer fakeCommentID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> commentService.deleteComment(fakeCommentID));

        //then
        assertEquals(COMMENT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void deleteComment_throwsForbiddenCommentAccessExceptionTest() {

        //given
        Integer commentID = createDefaultCommentForIssue();
        when(userService.getCurrentLoggedUser()).thenReturn(prepareAnotherUserMock());

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> commentService.deleteComment(commentID));

        //then
        assertEquals(FORBIDDEN_COMMENT_ACCESS, exception.getMessage());
    }

    private Integer createDefaultCommentForIssue() {

        Comment comment = Comment.builder()
                .body(RANDOM_COMMENT_BODY)
                .build();
        Date now = new Date();
        comment.setCreatedBy(CURRENT_USER_UUID);
        comment.setCreatedDate(now);
        comment.setLastModifiedBy(CURRENT_USER_UUID);
        comment.setLastModifiedDate(now);
        commentRepository.save(comment);

        Set<Comment> issueComments = issueEntity.getComments();
        issueComments.add(comment);
        issueRepository.save(issueEntity);

        return comment.getId();
    }
}
