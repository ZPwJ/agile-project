package io.github.student.agile.analytics;

import io.github.student.agile.builder.SprintBuilder;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.repository.BacklogRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithSprint;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@EnableJpaAuditing
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {TestConfiguration.class})
public class SprintMetricsRestControllerTest {

    private static final String BASE = "/api/analytics/sprints";

    @Autowired
    private MockMvc mvc;
    @MockBean
    private UserService userService;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private BacklogRepository backlogRepository;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private IssueStatusRepository issueStatusRepository;

    private Sprint sprint;

    @BeforeEach
    void setUp() {
        Project project = projectRepository.save(createProjectEntity());
        Backlog backlog = project.getBacklog();
        backlog.setProject(project);
        backlogRepository.save(backlog);

        Sprint s = SprintBuilder.createSprintEntity(project, true);
        s.setSprintIssues(new HashSet<>());
        sprint = sprintRepository.save(s);

        Issue done = createIssueEntityWithSprint(sprint);
        done.setEstimation(5);
        done.setIssueStatus(issueStatusRepository.findById(IssueStatusID.DONE.getStatusID()).orElseThrow());
        Issue open = createIssueEntityWithSprint(sprint);
        open.setEstimation(null);
        open.setIssueStatus(issueStatusRepository.findById(IssueStatusID.TO_DO.getStatusID()).orElseThrow());
        issueRepository.save(done);
        issueRepository.save(open);
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void summary_returnsMetricsOfThatSprint() throws Exception {
        mvc.perform(get(BASE + "/" + sprint.getId() + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId", is(sprint.getId())))
                .andExpect(jsonPath("$.sprintName", is(SprintBuilder.SPRINT_NAME)))
                .andExpect(jsonPath("$.projectId", is(sprint.getProject().getId())))
                .andExpect(jsonPath("$.state", is("ACTIVE")))
                .andExpect(jsonPath("$.unit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.totalTasks", is(2)))
                .andExpect(jsonPath("$.completedTasks", is(1)))
                .andExpect(jsonPath("$.incompleteTasks", is(1)))
                .andExpect(jsonPath("$.unestimatedTasks", is(1)))
                .andExpect(jsonPath("$.velocity", is(5)))
                .andExpect(jsonPath("$.incompleteEstimate", is(0)));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void velocity_returnsSumOfCompletedEstimations() throws Exception {
        mvc.perform(get(BASE + "/" + sprint.getId() + "/velocity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId", is(sprint.getId())))
                .andExpect(jsonPath("$.velocity", is(5)))
                .andExpect(jsonPath("$.unit", is("STORY_POINTS")));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void summary_unknownSprint_returns404WithApiError() throws Exception {
        mvc.perform(get(BASE + "/-1/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("-1")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void velocity_unknownSprint_returns404() throws Exception {
        mvc.perform(get(BASE + "/-1/velocity"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void summary_nonNumericId_returns400WithApiError() throws Exception {
        mvc.perform(get(BASE + "/abc/summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PARAMETER")));
    }
}
