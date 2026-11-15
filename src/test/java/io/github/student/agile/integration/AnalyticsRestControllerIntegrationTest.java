package io.github.student.agile.integration;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
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

import static io.github.student.agile.builder.IssueBuilder.createIssueEntity;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.SPRINT_NAME;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_ALREADY_FINISHED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
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
public class AnalyticsRestControllerIntegrationTest {

    private static final String ANALYTICS_BASE_URL = "/api/analytics";

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
    private IssueStatusRepository issueStatusRepository;

    @Autowired
    private SprintService sprintService;

    private Project project;

    @BeforeEach
    void setUp() {
        when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        project = projectRepository.save(createProjectEntity());
    }

    @Test
    public void getSprintReport_returnsReportOfGivenSprint() throws Exception {

        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));
        Issue estimated = createIssueEntity();
        estimated.setEstimation(5);
        Issue unestimated = createIssueEntity();
        unestimated.setEstimation(null);
        for (Issue issue : new Issue[]{estimated, unestimated}) {
            issue.setIssueStatus(issueStatusRepository.findById(TO_DO.getStatusID()).orElseThrow());
            issueRepository.save(issue);
            sprintService.addIssueToSprint(issue.getId(), sprint.getId());
        }
        sprintService.startSprint(sprint.getId());

        mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/report", sprint.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sprintID", is(sprint.getId())))
                .andExpect(jsonPath("$.sprintName", is(SPRINT_NAME)))
                .andExpect(jsonPath("$.projectID", is(project.getId())))
                .andExpect(jsonPath("$.state", is("ACTIVE")))
                .andExpect(jsonPath("$.startedAt", notNullValue()))
                .andExpect(jsonPath("$.historyAvailable", is(true)))
                .andExpect(jsonPath("$.initialIssueCount", is(2)))
                .andExpect(jsonPath("$.completedIssueCount", is(0)))
                .andExpect(jsonPath("$.uncompletedIssueCount", is(2)))
                .andExpect(jsonPath("$.velocity", is(0)))
                .andExpect(jsonPath("$.uncompletedEstimation", is(5)))
                .andExpect(jsonPath("$.estimationUnit", is("STORY_POINTS")));
    }

    @Test
    public void getSprintReport_notExistingSprint_returns404() throws Exception {

        mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/report", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is(SPRINT_DOES_NOT_EXISTS)))
                .andExpect(jsonPath("$.path", is(ANALYTICS_BASE_URL + "/sprints/-1/report")));
    }

    @Test
    public void getSprintReport_nonNumericSprintID_returns400() throws Exception {

        mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/report", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid value 'abc' for parameter 'sprintID'")));
    }

    @Test
    public void getSprintBurndown_returnsChronologicalPoints() throws Exception {

        Sprint sprint = startedSprintWithIssue(5);

        mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/burndown", sprint.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sprintID", is(sprint.getId())))
                .andExpect(jsonPath("$.projectID", is(project.getId())))
                .andExpect(jsonPath("$.state", is("ACTIVE")))
                .andExpect(jsonPath("$.historyAvailable", is(true)))
                .andExpect(jsonPath("$.initialScope", is(5)))
                .andExpect(jsonPath("$.estimationUnit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.timeZone", is("Europe/Warsaw")))
                .andExpect(jsonPath("$.points[0].day", is(0)))
                .andExpect(jsonPath("$.points[0].date", matchesPattern("\\d{4}-\\d{2}-\\d{2}")))
                .andExpect(jsonPath("$.points[0].remainingWork", is(5)))
                .andExpect(jsonPath("$.points[1].day", is(1)));
    }

    @Test
    public void getSprintBurnup_returnsChronologicalPoints() throws Exception {

        Sprint sprint = startedSprintWithIssue(3);

        mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/burnup", sprint.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sprintID", is(sprint.getId())))
                .andExpect(jsonPath("$.historyAvailable", is(true)))
                .andExpect(jsonPath("$.estimationUnit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.timeZone", is("Europe/Warsaw")))
                .andExpect(jsonPath("$.points[0].day", is(0)))
                .andExpect(jsonPath("$.points[0].completedWork", is(0)))
                .andExpect(jsonPath("$.points[0].totalScope", is(3)))
                .andExpect(jsonPath("$.points[1].day", is(1)));
    }

    @Test
    public void getSprintCharts_plannedSprint_returnsEmptyPoints() throws Exception {

        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));

        for (String chart : new String[]{"burndown", "burnup"}) {
            mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/" + chart, sprint.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state", is("PLANNED")))
                    .andExpect(jsonPath("$.historyAvailable", is(false)))
                    .andExpect(jsonPath("$.points", hasSize(0)));
        }
    }

    @Test
    public void getSprintCharts_notExistingSprint_returns404() throws Exception {

        for (String chart : new String[]{"burndown", "burnup"}) {
            mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/" + chart, -1))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is(SPRINT_DOES_NOT_EXISTS)))
                    .andExpect(jsonPath("$.path", is(ANALYTICS_BASE_URL + "/sprints/-1/" + chart)));
        }
    }

    @Test
    public void getSprintCharts_nonNumericSprintID_returns400() throws Exception {

        for (String chart : new String[]{"burndown", "burnup"}) {
            mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/" + chart, "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Invalid value 'abc' for parameter 'sprintID'")));
        }
    }

    private Sprint startedSprintWithIssue(Integer estimation) {
        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));
        Issue issue = createIssueEntity();
        issue.setEstimation(estimation);
        issue.setIssueStatus(issueStatusRepository.findById(TO_DO.getStatusID()).orElseThrow());
        issueRepository.save(issue);
        sprintService.addIssueToSprint(issue.getId(), sprint.getId());
        sprintService.startSprint(sprint.getId());
        return sprint;
    }

    @Test
    public void addIssueToFinishedSprint_returns409() throws Exception {

        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));
        sprint.setIsDone(Boolean.TRUE);
        Issue issue = createIssueEntity();
        issue.setIssueStatus(issueStatusRepository.findById(TO_DO.getStatusID()).orElseThrow());
        issueRepository.save(issue);

        mvc.perform(post("/api/sprint/{sprintID}/issue/{issueID}", sprint.getId(), issue.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is(SPRINT_ALREADY_FINISHED)));
    }

    @Test
    public void startNotExistingSprint_returns404() throws Exception {

        mvc.perform(patch("/api/sprint/start/{id}", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(SPRINT_DOES_NOT_EXISTS)));
    }
}
