package io.github.student.agile.integration;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.controller.ApiExceptionHandler;
import io.github.student.agile.resource.controller.SprintReportRestController;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.SprintReportDTO;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
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

import java.util.HashSet;

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
