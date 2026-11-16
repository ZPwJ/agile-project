package io.github.student.agile.integration;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.controller.ApiExceptionHandler;
import io.github.student.agile.resource.controller.SprintReportRestController;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.dto.BurndownPointDTO;
import io.github.student.agile.resource.dto.SprintReportDTO;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintProgressRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.SprintReportService;
import io.github.student.agile.resource.service.SprintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
class SprintReportIntegrationTest {

    @Autowired private SprintReportService reports;
    @Autowired private SprintService sprintService;
    @Autowired private IssueService issueService;
    @Autowired private ProjectRepository projects;
    @Autowired private SprintRepository sprints;
    @Autowired private IssueRepository issues;
    @Autowired private IssueStatusRepository statuses;
    @Autowired private SprintProgressRepository progress;

    private MockMvc mvc;
    private Project project;

    @BeforeEach
    void setUp() {
        project = projects.save(Project.builder().name("Reports").build());
        mvc = MockMvcBuilders.standaloneSetup(new SprintReportRestController(reports))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))
                .build();
    }

    @Test
    void returnsFrozenResultWithNullEstimatesAndIgnoresOtherSprintsAndProjects() throws Exception {
        Sprint sprint = sprint(project, "First");
        Issue done = issue(sprint, 5, 3);
        issue(sprint, 5, null);
        issue(sprint, 2, null);
        sprintService.startSprint(sprint.getId());
        Issue added = issue(null, 1, 8);
        sprintService.addIssueToSprint(added.getId(), sprint.getId());
        reestimate(done, 5);
        sprintService.stopSprint(project.getId());

        Sprint other = sprint(projects.save(Project.builder().name("Other project").build()), "Other");
        issue(other, 5, 100);
        sprintService.startSprint(other.getId());
        sprintService.stopSprint(other.getProject().getId());

        SprintReportDTO original = reports.getReport(sprint.getId());
        reestimate(done, 99);
        Sprint next = sprint(project, "Next");
        sprintService.addIssueToSprint(added.getId(), next.getId());
        sprintService.startSprint(next.getId());
        reestimate(added, 13);
        sprintService.stopSprint(project.getId());
        assertThat(reports.getReport(sprint.getId())).isEqualTo(original);
        assertThat(reports.getReport(other.getId()).velocity()).isEqualTo(100);
        assertThat(reports.getReport(next.getId()).velocity()).isZero();
        assertThat(reports.getReport(next.getId()).incompletePoints()).isEqualTo(13);

        mvc.perform(get("/api/reports/sprints/{id}", sprint.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintID").value(sprint.getId()))
                .andExpect(jsonPath("$.projectID").value(project.getId()))
                .andExpect(jsonPath("$.name").value("First"))
                .andExpect(jsonPath("$.startDate").value(sprint.getStartDate().toInstant().toString()))
                .andExpect(jsonPath("$.endDate").value(sprint.getEndDate().toInstant().toString()))
                .andExpect(jsonPath("$.initialIssueCount").value(3))
                .andExpect(jsonPath("$.completedIssueCount").value(2))
                .andExpect(jsonPath("$.incompleteIssueCount").value(2))
                .andExpect(jsonPath("$.velocity").value(5))
                .andExpect(jsonPath("$.incompletePoints").value(8));
    }

    @Test
    void returns404ForMissingSprintAnd400ForInvalidIdentifier() throws Exception {
        mvc.perform(get("/api/reports/sprints/2147483647"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Sprint with given ID does not exists"));
        mvc.perform(get("/api/reports/sprints/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/sprints/text")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/sprints/2147483647/burndown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sprint with given ID does not exists"));
        mvc.perform(get("/api/reports/sprints/0/burndown")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/sprints/text/burndown")).andExpect(status().isBadRequest());
    }

    @Test
    void returns409ForUnfinishedSprintOrMissingFinalHistory() throws Exception {
        Sprint sprint = sprint(project, "No result");
        mvc.perform(get("/api/reports/sprints/{id}", sprint.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sprint report requires a closed sprint"));
        sprint.setActive(true);
        mvc.perform(get("/api/reports/sprints/{id}", sprint.getId())).andExpect(status().isConflict());
        sprint.setActive(false);
        sprint.setIsDone(true);
        mvc.perform(get("/api/reports/sprints/{id}", sprint.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sprint has no recorded final result"));
    }

    @Test
    void burndownTracksScopeEstimatesAndCompletionWithoutMixingProjects() throws Exception {
        Sprint sprint = sprint(project, "Burndown");
        Issue first = issue(sprint, 1, 3);
        issue(sprint, 1, 2);
        issue(sprint, 1, null);
        sprintService.startSprint(sprint.getId());
        Issue added = issue(null, 1, 5);
        sprintService.addIssueToSprint(added.getId(), sprint.getId());
        reestimate(first, 8);
        for (int status = 2; status <= 5; status++) {
            issueService.changeIssueStatus(first.getId(), status);
        }
        sprintService.deleteIssueFromSprint(sprint.getId(), added.getId());

        Sprint other = sprint(projects.save(Project.builder().name("Other burndown project").build()), "Other");
        issue(other, 1, 100);
        sprintService.startSprint(other.getId());

        List<BurndownPointDTO> activePoints = reports.getBurndown(sprint.getId());
        assertThat(activePoints).extracting(BurndownPointDTO::remainingPoints)
                .containsExactly(5L, 10L, 15L, 15L, 15L, 15L, 7L, 2L);
        sprintService.stopSprint(project.getId());
        List<BurndownPointDTO> finalPoints = reports.getBurndown(sprint.getId());
        assertThat(finalPoints).extracting(BurndownPointDTO::remainingPoints)
                .containsExactly(5L, 10L, 15L, 15L, 15L, 15L, 7L, 2L, 2L);
        assertThat(finalPoints).extracting(BurndownPointDTO::timestamp).isSorted();
        reestimate(first, 99);
        assertThat(reports.getBurndown(sprint.getId())).isEqualTo(finalPoints);

        mvc.perform(get("/api/reports/sprints/{id}/burndown", sprint.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[0].timestamp").value(sprint.getStartDate().toInstant().toString()))
                .andExpect(jsonPath("$[0].remainingPoints").value(5))
                .andExpect(jsonPath("$[8].remainingPoints").value(2));
    }

    @Test
    void burndownReturnsEmptyHistoryAndZeroForUnestimatedWork() throws Exception {
        Sprint sprint = sprint(project, "Unestimated");
        mvc.perform(get("/api/reports/sprints/{id}/burndown", sprint.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        Issue unestimated = issue(sprint, 1, null);
        sprintService.startSprint(sprint.getId());
        for (int status = 2; status <= 5; status++) {
            issueService.changeIssueStatus(unestimated.getId(), status);
        }
        sprintService.stopSprint(project.getId());

        assertThat(reports.getBurndown(sprint.getId())).hasSize(6)
                .extracting(BurndownPointDTO::remainingPoints).containsOnly(0L);
    }

    @Test
    void burndownOrdersRecordedTimesAndBreaksTiesById() {
        Sprint sprint = sprint(project, "Same-day completions");
        ZonedDateTime time = ZonedDateTime.parse("2026-11-01T10:00:00Z");
        progress.save(SprintProgress.builder().sprint(sprint).kind(SprintProgress.Kind.END)
                .capturedAt(time.plusHours(1)).totalPoints(10).completedPoints(8).build());
        progress.save(SprintProgress.builder().sprint(sprint).kind(SprintProgress.Kind.CHANGE)
                .capturedAt(time).totalPoints(10).completedPoints(3).build());
        progress.save(SprintProgress.builder().sprint(sprint).kind(SprintProgress.Kind.CHANGE)
                .capturedAt(time).totalPoints(10).completedPoints(5).build());

        assertThat(reports.getBurndown(sprint.getId())).containsExactly(
                new BurndownPointDTO(time.toInstant(), 7),
                new BurndownPointDTO(time.toInstant(), 5),
                new BurndownPointDTO(time.plusHours(1).toInstant(), 2));
    }

    private Sprint sprint(Project owner, String name) {
        return sprints.save(Sprint.builder().name(name).project(owner).active(false).isDone(false)
                .sprintIssues(new HashSet<>()).build());
    }

    private Issue issue(Sprint sprint, int status, Integer estimation) {
        Issue issue = issues.save(Issue.builder().name("Work").description("Work")
                .projectID(sprint == null ? project.getId() : sprint.getProject().getId())
                .sprint(sprint).issueStatus(statuses.getReferenceById(status)).estimation(estimation).build());
        if (sprint != null) {
            sprint.getSprintIssues().add(issue);
        }
        return issue;
    }

    private void reestimate(Issue issue, int estimation) {
        issueService.updateIssue(IssueMRO.builder().issueID(issue.getId()).name("Work")
                .description("Work").estimation(estimation).build());
    }
}
