package io.github.student.agile.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.SprintService;
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

import static io.github.student.agile.builder.IssueBuilder.OTHER_ISSUE_DESCRIPTION;
import static io.github.student.agile.builder.IssueBuilder.OTHER_ISSUE_ESTIMATION;
import static io.github.student.agile.builder.IssueBuilder.OTHER_ISSUE_NAME;
import static io.github.student.agile.builder.IssueBuilder.createDefaultIssue;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.UserBuilder.CURRENT_USER_UUID;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_EMPTY;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NEGATIVE_ESTIMATION;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NULL_IDENTIFIER;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
public class IssueRestControllerIntegrationTest {

    private static final String ISSUE_BASE_URL = "/api/issue";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private SprintService sprintService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Project project;

    @BeforeEach
    void setUp() {
        when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        project = projectRepository.save(createProjectEntity());
    }

    @Test
    public void createIssue_returnsCreatedIssueInToDo() throws Exception {

        IssueMRO body = createDefaultIssue(project.getId());

        mvc.perform(post(ISSUE_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(body.getName())))
                .andExpect(jsonPath("$.estimation", is(body.getEstimation())))
                .andExpect(jsonPath("$.projectID", is(project.getId())))
                .andExpect(jsonPath("$.issueStatusID", is(TO_DO.getStatusID())))
                .andExpect(jsonPath("$.issueTypeID", is(body.getIssueTypeID())))
                .andExpect(jsonPath("$.issuePriorityID", is(body.getIssuePriorityID())));

        assertThat(issueRepository.findAll()).extracting(Issue::getReporterID).containsExactly(CURRENT_USER_UUID);
    }

    @Test
    public void createIssue_notExistingProject_returns404() throws Exception {

        mvc.perform(post(ISSUE_BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultIssue(-1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(PROJECT_DOES_NOT_EXISTS)));
    }

    @Test
    public void createIssue_negativeEstimation_returns400() throws Exception {

        IssueMRO body = createDefaultIssue(project.getId());
        body.setEstimation(-1);

        mvc.perform(post(ISSUE_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(NEGATIVE_ESTIMATION)));
    }

    @Test
    public void getIssuesAndProductBacklog_returnProjectIssues() throws Exception {

        Integer backlogIssueID = issueService.createIssue(createDefaultIssue(project.getId())).getIssueID();
        Integer sprintIssueID = issueService.createIssue(createDefaultIssue(project.getId())).getIssueID();
        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));
        sprintService.addIssueToSprint(sprintIssueID, sprint.getId());

        mvc.perform(get(ISSUE_BASE_URL).param("projectID", String.valueOf(project.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get(ISSUE_BASE_URL + "/product-backlog").param("projectID", String.valueOf(project.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(backlogIssueID)));

        mvc.perform(get(ISSUE_BASE_URL + "/sprint/{id}", sprint.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(sprintIssueID)))
                .andExpect(jsonPath("$[0].sprintID", is(sprint.getId())));
    }

    @Test
    public void getProductBacklogHistory_returns200() throws Exception {

        issueService.createIssue(createDefaultIssue(project.getId()));

        mvc.perform(get(ISSUE_BASE_URL + "/product-backlog-history").param("projectID", String.valueOf(project.getId())))
                .andExpect(status().isOk());
    }

    @Test
    public void getIssues_withoutProjectID_returns400() throws Exception {

        mvc.perform(get(ISSUE_BASE_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    public void getIssues_notExistingProject_returns404() throws Exception {

        for (String path : new String[]{"", "/product-backlog", "/product-backlog-history"}) {
            mvc.perform(get(ISSUE_BASE_URL + path).param("projectID", "-1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is(PROJECT_DOES_NOT_EXISTS)));
        }
    }

    @Test
    public void getIssueDetails_returnsIssue() throws Exception {

        Integer issueID = issueService.createIssue(createDefaultIssue(project.getId())).getIssueID();

        mvc.perform(get(ISSUE_BASE_URL + "/details/{id}", issueID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueID", is(issueID)))
                .andExpect(jsonPath("$.comments", hasSize(0)));
    }

    @Test
    public void getIssueDetails_notExistingIssue_returns404() throws Exception {

        mvc.perform(get(ISSUE_BASE_URL + "/details/{id}", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(ISSUE_DOES_NOT_EXISTS)));
    }

    @Test
    public void updateIssue_returnsUpdatedIssue() throws Exception {

        Integer issueID = issueService.createIssue(createDefaultIssue(project.getId())).getIssueID();
        IssueMRO body = createDefaultIssue(project.getId());
        body.setIssueID(issueID);
        body.setName(OTHER_ISSUE_NAME);
        body.setDescription(OTHER_ISSUE_DESCRIPTION);
        body.setEstimation(OTHER_ISSUE_ESTIMATION);

        mvc.perform(put(ISSUE_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueID", is(issueID)))
                .andExpect(jsonPath("$.name", is(OTHER_ISSUE_NAME)))
                .andExpect(jsonPath("$.description", is(OTHER_ISSUE_DESCRIPTION)))
                .andExpect(jsonPath("$.estimation", is(OTHER_ISSUE_ESTIMATION)));
    }

    @Test
    public void updateIssue_withoutIssueID_returns400() throws Exception {

        mvc.perform(put(ISSUE_BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDefaultIssue(project.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(NULL_IDENTIFIER)));
    }

    @Test
    public void deleteIssue_removesIssue() throws Exception {

        Integer issueID = issueService.createIssue(createDefaultIssue(project.getId())).getIssueID();

        mvc.perform(delete(ISSUE_BASE_URL + "/{id}", issueID))
                .andExpect(status().isOk());

        assertThat(issueRepository.findById(issueID)).isEmpty();
    }

    @Test
    public void deleteIssue_notExistingIssue_returns404() throws Exception {

        mvc.perform(delete(ISSUE_BASE_URL + "/{id}", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(ISSUE_DOES_NOT_EXISTS)));
    }

    @Test
    public void assignAndUnassignCurrentUser_changesUserAssignedIssues() throws Exception {

        Integer issueID = issueService.createIssue(createDefaultIssue(project.getId())).getIssueID();

        mvc.perform(post(ISSUE_BASE_URL + "/{id}/assign", issueID))
                .andExpect(status().isOk());
        mvc.perform(get(ISSUE_BASE_URL + "/user-assigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(issueID)));

        mvc.perform(put(ISSUE_BASE_URL + "/{id}/delete-assignment", issueID))
                .andExpect(status().isOk());
        mvc.perform(get(ISSUE_BASE_URL + "/user-assigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void assignCurrentUser_notExistingIssue_returns404() throws Exception {

        mvc.perform(post(ISSUE_BASE_URL + "/{id}/assign", -1))
                .andExpect(status().isNotFound());
        mvc.perform(put(ISSUE_BASE_URL + "/{id}/delete-assignment", -1))
                .andExpect(status().isNotFound());
    }

    @Test
    public void changeIssueStatus_issueInActiveSprint_changesStatus() throws Exception {

        Integer issueID = issueService.createIssue(createDefaultIssue(project.getId())).getIssueID();
        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));
        sprintService.addIssueToSprint(issueID, sprint.getId());
        sprintService.startSprint(sprint.getId());

        mvc.perform(patch(ISSUE_BASE_URL + "/{id}/status/{statusID}", issueID, IN_PROGRESS.getStatusID()))
                .andExpect(status().isOk());

        assertThat(issueRepository.findById(issueID).orElseThrow().getIssueStatus().getId()).isEqualTo(IN_PROGRESS.getStatusID());
    }

    @Test
    public void changeIssueStatus_issueOutsideSprint_returns409() throws Exception {

        Integer issueID = issueService.createIssue(createDefaultIssue(project.getId())).getIssueID();

        mvc.perform(patch(ISSUE_BASE_URL + "/{id}/status/{statusID}", issueID, IN_PROGRESS.getStatusID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is(IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_EMPTY)));
    }
}
