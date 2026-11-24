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
import jakarta.persistence.EntityManager;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithIssueStatus;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfiguration.class)
class SprintAnalyticsIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private SprintRepository sprintRepository;
    @Autowired private IssueRepository issueRepository;
    @Autowired private IssueStatusRepository statusRepository;
    @Autowired private SprintService sprintService;
    @Autowired private EntityManager entityManager;
    @MockBean private UserService userService;

    private Project project;

    @BeforeEach
    void setUp() {
        when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        project = projectRepository.save(createProjectEntity());
    }

    @Test
    void projectSprintComparisonAndCsvRespectFilters() throws Exception {
        ZonedDateTime jan = ZonedDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime feb = ZonedDateTime.of(2024, 2, 15, 0, 0, 0, 0, ZoneOffset.UTC);
        Sprint janSprint = sprintRepository.save(createSprintEntity(project, false));
        janSprint.setStartDate(jan);
        sprintRepository.save(janSprint);
        Sprint febSprint = sprintRepository.save(createSprintEntity(project, false));
        febSprint.setName("February sprint");
        febSprint.setStartDate(feb);
        sprintRepository.save(febSprint);
        issue(janSprint, DONE.getStatusID(), 4);

        mvc.perform(get("/api/analytics/projects/{id}/sprints", project.getId())
                        .param("from", LocalDate.of(2024, 1, 1).toString())
                        .param("to", LocalDate.of(2024, 1, 31).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sprintId").value(janSprint.getId()))
                .andExpect(jsonPath("$[0].completedIssueCount").value(1))
                .andExpect(jsonPath("$[0].velocity").value(4))
                .andExpect(jsonPath("$[0].estimationUnit").value("STORY_POINTS"));

        mvc.perform(get("/api/analytics/projects/{id}/sprints", 999999))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/analytics/projects/{id}/sprints", project.getId())
                        .param("from", "2024-02-01")
                        .param("to", "2024-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'from' must not be after 'to'"));

        mvc.perform(get("/api/analytics/projects/{id}/sprints/export.csv", project.getId())
                        .param("from", LocalDate.of(2024, 1, 1).toString())
                        .param("to", LocalDate.of(2024, 1, 31).toString()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"project-" + project.getId() + "-sprints.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith(
                        "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,incompleteIssueCount,velocity\r\n")));
    }

    @Test
    void missingSprintAndInvalidIdsReturnJsonErrors() throws Exception {
        mvc.perform(get("/api/analytics/sprints/999999/report"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Sprint not found: 999999"));
        mvc.perform(get("/api/analytics/sprints/abc/report"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
        mvc.perform(get("/api/analytics/sprints/0/report"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void emptySprintHasZeroMetrics() throws Exception {
        Sprint sprint = sprintRepository.save(createSprintEntity(project, false));
        mvc.perform(get("/api/analytics/sprints/{id}/report", sprint.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId").value(sprint.getId()))
                .andExpect(jsonPath("$.sprintName").value(sprint.getName()))
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.startDate").exists())
                .andExpect(jsonPath("$.totalIssues").value(0))
                .andExpect(jsonPath("$.completedIssues").value(0))
                .andExpect(jsonPath("$.incompleteIssues").value(0))
                .andExpect(jsonPath("$.velocity").value(0))
                .andExpect(jsonPath("$.incompleteEstimation").value(0));
    }

    @Test
    void reportRetainsIncompleteSnapshotAfterSprintStops() throws Exception {
        Sprint sprint = sprintRepository.save(createSprintEntity(project, false));
        Issue done = issue(sprint, DONE.getStatusID(), 5);
        Issue doneWithoutEstimation = issue(sprint, DONE.getStatusID(), null);
        Issue incomplete = issue(sprint, IN_PROGRESS.getStatusID(), 8);
        Issue incompleteWithoutEstimation = issue(sprint, IN_PROGRESS.getStatusID(), null);
        Issue withoutStatus = createIssueEntityWithIssueStatus(null);
        withoutStatus.setEstimation(null);
        withoutStatus.setSprint(sprint);
        issueRepository.save(withoutStatus);
        sprint.setSprintIssues(new HashSet<>(Set.of(done, doneWithoutEstimation, incomplete, incompleteWithoutEstimation, withoutStatus)));

        sprintService.startSprint(sprint.getId());
        assertReport(sprint.getId());
        sprintService.stopSprint(project.getId());
        entityManager.flush();
        entityManager.clear();

        Sprint persistedSprint = sprintRepository.findById(sprint.getId()).orElseThrow();
        assertEquals(5, persistedSprint.getInitialIssueCount());
        assertEquals(13, persistedSprint.getInitialEstimation());
        assertEquals(3, persistedSprint.getIncompleteIssueCount());
        assertEquals(8, persistedSprint.getIncompleteEstimation());
        assertReport(sprint.getId());
        mvc.perform(get("/api/analytics/sprints/{id}/report", sprint.getId()))
                .andExpect(jsonPath("$.endDate").exists());
    }

    private void assertReport(Integer id) throws Exception {
        mvc.perform(get("/api/analytics/sprints/{id}/report", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIssues").value(5))
                .andExpect(jsonPath("$.completedIssues").value(2))
                .andExpect(jsonPath("$.incompleteIssues").value(3))
                .andExpect(jsonPath("$.velocity").value(5))
                .andExpect(jsonPath("$.incompleteEstimation").value(8));
    }

    private Issue issue(Sprint sprint, Integer statusId, Integer estimation) {
        Issue issue = createIssueEntityWithIssueStatus(statusRepository.getReferenceById(statusId));
        issue.setEstimation(estimation);
        issue.setSprint(sprint);
        return issueRepository.save(issue);
    }
}
