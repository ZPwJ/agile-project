package io.github.student.agile.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Comment;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.dto.request.CommentMRO;
import io.github.student.agile.resource.repository.CommentRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.service.CommentService;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static io.github.student.agile.builder.IssueBuilder.createDefaultIssue;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.UserBuilder.CURRENT_USER_UUID;
import static io.github.student.agile.builder.UserBuilder.SECOND_USER_UUID;
import static io.github.student.agile.builder.UserBuilder.createAnotherCurrentLoggedInUser;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.COMMENT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.FORBIDDEN_COMMENT_ACCESS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_DOES_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@EnableJpaAuditing
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {TestConfiguration.class})
public class CommentRestControllerIntegrationTest {

    private static final String COMMENT_BASE_URL = "/api/comment";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private CommentService commentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Integer issueID;

    @BeforeEach
    void setUp() {
        when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        Project project = projectRepository.save(createProjectEntity());
        issueID = issueService.createIssue(createDefaultIssue(project.getId())).getIssueID();
    }

    @Test
    public void addComment_returnsCommentAndAttachesItToIssue() throws Exception {

        mvc.perform(post(COMMENT_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body(null, issueID, "First comment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body", is("First comment")));

        assertThat(issueRepository.findById(issueID).orElseThrow().getComments())
                .extracting(Comment::getBody).containsExactly("First comment");
    }

    @Test
    public void addComment_notExistingIssue_returns404() throws Exception {

        mvc.perform(post(COMMENT_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body(null, -1, "Comment")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(ISSUE_DOES_NOT_EXISTS)));
    }

    @Test
    public void updateComment_ownComment_returnsUpdatedComment() throws Exception {

        Integer commentID = commentOfCurrentUser("Before");

        mvc.perform(put(COMMENT_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body(commentID, issueID, "After")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentID", is(commentID)))
                .andExpect(jsonPath("$.body", is("After")));
    }

    @Test
    public void updateComment_commentOfAnotherUser_returns403() throws Exception {

        Integer commentID = commentOfCurrentUser("Before");
        when(userService.getCurrentLoggedUser()).thenReturn(createAnotherCurrentLoggedInUser(SECOND_USER_UUID));

        mvc.perform(put(COMMENT_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body(commentID, issueID, "After")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is(FORBIDDEN_COMMENT_ACCESS)));

        assertThat(commentRepository.findById(commentID).orElseThrow().getBody()).isEqualTo("Before");
    }

    @Test
    public void updateComment_notExistingComment_returns404() throws Exception {

        mvc.perform(put(COMMENT_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body(-1, issueID, "After")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(COMMENT_DOES_NOT_EXISTS)));
    }

    @Test
    public void deleteComment_ownComment_removesComment() throws Exception {

        Integer commentID = commentOfCurrentUser("To delete");

        mvc.perform(delete(COMMENT_BASE_URL + "/{id}", commentID))
                .andExpect(status().isOk());

        assertThat(commentRepository.findById(commentID)).isEmpty();
        assertThat(issueRepository.findById(issueID).orElseThrow().getComments()).isEmpty();
    }

    @Test
    public void deleteComment_commentOfAnotherUser_returns403() throws Exception {

        Integer commentID = commentOfCurrentUser("Keep");
        when(userService.getCurrentLoggedUser()).thenReturn(createAnotherCurrentLoggedInUser(SECOND_USER_UUID));

        mvc.perform(delete(COMMENT_BASE_URL + "/{id}", commentID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is(FORBIDDEN_COMMENT_ACCESS)));

        assertThat(commentRepository.findById(commentID)).isPresent();
    }

    @Test
    public void deleteComment_notExistingComment_returns404() throws Exception {

        mvc.perform(delete(COMMENT_BASE_URL + "/{id}", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(COMMENT_DOES_NOT_EXISTS)));
    }

    private Integer commentOfCurrentUser(String text) {
        Integer commentID = commentService.addCommentToIssue(new CommentMRO(null, issueID, text)).getCommentID();
        Comment comment = commentRepository.findById(commentID).orElseThrow();
        comment.setCreatedBy(CURRENT_USER_UUID);
        commentRepository.save(comment);
        return commentID;
    }

    private String body(Integer commentID, Integer issueID, String text) throws Exception {
        return objectMapper.writeValueAsString(new CommentMRO(commentID, issueID, text));
    }
}
