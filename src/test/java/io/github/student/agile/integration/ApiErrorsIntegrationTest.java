package io.github.student.agile.integration;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.controller.ApiExceptionHandler;
import io.github.student.agile.resource.controller.IssueRestController;
import io.github.student.agile.resource.controller.SprintRestController;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.request.ReportDateRange;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.SprintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
class ApiErrorsIntegrationTest {

    @Autowired private SprintService sprintService;
    @Autowired private IssueService issueService;
    @Autowired private ProjectRepository projects;
    @Autowired private SprintRepository sprints;
    @Autowired private IssueRepository issues;
    @Autowired private IssueStatusRepository statuses;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SprintRestController(sprintService),
                new IssueRestController(issueService), new FilterTestController())
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void missingProjectAndSprintReturn404WithReadableJson() throws Exception {
        mvc.perform(get("/api/sprint/active/2147483647"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project with given ID does not exists"))
                .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(patch("/api/sprint/start/2147483647"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sprint with given ID does not exists"));
    }

    @Test
    void missingStatusReturns404() throws Exception {
        Project project = projects.save(Project.builder().name("Errors").build());
        Sprint sprint = sprints.save(Sprint.builder().name("Active").project(project)
                .active(true).isDone(false).build());
        Issue issue = issues.save(Issue.builder().sprint(sprint)
                .issueStatus(statuses.getReferenceById(1)).build());
        mvc.perform(patch("/api/issue/{id}/status/2147483647", issue.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Issue status with given ID does not exists"));
    }

    @Test
    void invalidParametersAndJsonReturn400() throws Exception {
        mvc.perform(patch("/api/sprint/start/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Identifier must be positive"));
        mvc.perform(patch("/api/sprint/start/text"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/issue"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/sprint").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/sprint").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sprint\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Identifier cannot be null value"));
        mvc.perform(post("/api/issue").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Work\",\"estimation\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Estimation cannot be negative"));
    }

    @Test
    void closedSprintReturns409() throws Exception {
        Project project = projects.save(Project.builder().name("Errors").build());
        Sprint sprint = sprints.save(Sprint.builder().name("Closed").project(project)
                .active(false).isDone(true).sprintIssues(new HashSet<>()).build());
        mvc.perform(patch("/api/sprint/start/{id}", sprint.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Closed sprint cannot be changed or restarted"));
    }

    @Test
    void reportDateRangeRejectsReversedAndMalformedBounds() throws Exception {
        mvc.perform(get("/test/report-filter").param("from", "2026-11-12").param("to", "2026-11-11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must not be after to"));
        mvc.perform(get("/test/report-filter").param("from", "2026-02-30"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/test/report-filter").param("from", "2026-11-11").param("to", "2026-11-11"))
                .andExpect(status().isOk());
        mvc.perform(get("/test/report-filter").param("to", "2026-11-11"))
                .andExpect(status().isOk());
        mvc.perform(get("/test/report-filter"))
                .andExpect(status().isOk());
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalDetails() throws Exception {
        String body = mvc.perform(get("/test/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("database connection", "trace", "exception");
    }

    // Only a test endpoint: reporting routes are implemented in their own tasks.
    @RestController
    static class FilterTestController {
        @GetMapping("/test/report-filter")
        ReportDateRange filter(
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
            return new ReportDateRange(from, to);
        }

        @GetMapping("/test/failure")
        void fail() {
            throw new IllegalStateException("database connection details");
        }
    }
}
