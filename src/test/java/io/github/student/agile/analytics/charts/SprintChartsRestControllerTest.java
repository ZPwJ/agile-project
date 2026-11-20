package io.github.student.agile.analytics.charts;

import com.jayway.jsonpath.JsonPath;
import io.github.student.agile.builder.SprintBuilder;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.IssueStatusHistory;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.repository.BacklogRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusHistoryRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithSprint;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WF-3 burndown and WF-4 burnup endpoints: JSON shapes of plan sections 2.1/2.2, ascending
 * non-negative points, and the existing analytics error body for 404/400.
 */
@Transactional
@EnableJpaAuditing
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {TestConfiguration.class})
public class SprintChartsRestControllerTest {

    private static final String BASE = "/api/analytics/sprints";
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static final LocalDate END = LocalDate.of(2026, 11, 6);

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
    @Autowired
    private IssueStatusHistoryRepository historyRepository;

    private Project project;
    private Sprint sprint;
    private Issue doneTask;

    @BeforeEach
    void setUp() {
        project = projectRepository.save(createProjectEntity());
        Backlog backlog = project.getBacklog();
        backlog.setProject(project);
        backlogRepository.save(backlog);

        sprint = createSprint(project);
        coverHistoryFrom2020();

        doneTask = createIssue(sprint, 5, IssueStatusID.DONE);
        // The only work of the sprint is finished on the third day of the axis.
        recordChange(sprint, doneTask, IssueStatusID.DONE, "2026-11-04T10:00:00Z");
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burndown_returnsAscendingPointsWithContractFields() throws Exception {
        mvc.perform(get(BASE + "/" + sprint.getId() + "/burndown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId", is(sprint.getId())))
                .andExpect(jsonPath("$.unit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.dataCompleteness", is("COMPLETE")))
                .andExpect(jsonPath("$.warnings").isArray())
                .andExpect(jsonPath("$.points", hasSize(5)))
                .andExpect(jsonPath("$.points[0].date", is("2026-11-02")))
                .andExpect(jsonPath("$.points[4].date", is("2026-11-06")))
                .andExpect(jsonPath("$.points[*].remaining", everyItem(greaterThanOrEqualTo(0))));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burndown_knownSprint_remainingDecreasesAfterWorkIsDone() throws Exception {
        mvc.perform(get(BASE + "/" + sprint.getId() + "/burndown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[0].remaining", is(5)))
                .andExpect(jsonPath("$.points[1].remaining", is(5)))
                .andExpect(jsonPath("$.points[2].remaining", is(0)))
                .andExpect(jsonPath("$.points[3].remaining", is(0)))
                .andExpect(jsonPath("$.points[4].remaining", is(0)));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burndown_unknownSprint_returns404WithApiError() throws Exception {
        mvc.perform(get(BASE + "/-1/burndown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("-1")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burndown_nonNumericId_returns400WithApiError() throws Exception {
        mvc.perform(get(BASE + "/abc/burndown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PARAMETER")))
                .andExpect(jsonPath("$.message", containsString("sprintId")));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burnup_completedIsCumulativeAndScopeIsConstant() throws Exception {
        // Second completion on another day: 5 points on 2026-11-04, 3 points on 2026-11-05.
        Issue second = createIssue(sprint, 3, IssueStatusID.DONE);
        recordChange(sprint, second, IssueStatusID.DONE, "2026-11-05T10:00:00Z");

        mvc.perform(get(BASE + "/" + sprint.getId() + "/burnup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(5)))
                .andExpect(jsonPath("$.points[0].completed", is(0)))
                .andExpect(jsonPath("$.points[1].completed", is(0)))
                .andExpect(jsonPath("$.points[2].completed", is(5)))
                .andExpect(jsonPath("$.points[3].completed", is(8)))
                .andExpect(jsonPath("$.points[4].completed", is(8)))
                .andExpect(jsonPath("$.points[*].scope", everyItem(is(8))));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burnup_unknownSprint_returns404() throws Exception {
        mvc.perform(get(BASE + "/-1/burnup"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("-1")));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burnup_lastPointMatchesSummaryVelocity() throws Exception {
        String burnup = mvc.perform(get(BASE + "/" + sprint.getId() + "/burnup"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String summary = mvc.perform(get(BASE + "/" + sprint.getId() + "/summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> points = JsonPath.read(burnup, "$.points");
        long lastCompleted = ((Number) points.get(points.size() - 1).get("completed")).longValue();
        long velocity = ((Number) JsonPath.read(summary, "$.velocity")).longValue();

        assertEquals(velocity, lastCompleted);
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burnup_warningsContainScopeNotHistorical() throws Exception {
        String burndown = mvc.perform(get(BASE + "/" + sprint.getId() + "/burndown"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String burnup = mvc.perform(get(BASE + "/" + sprint.getId() + "/burnup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(5)))
                .andExpect(jsonPath("$.warnings", hasItem(startsWith("SCOPE_NOT_HISTORICAL"))))
                .andReturn().getResponse().getContentAsString();

        // E12 applies to both chart types, and burnup adds no warning of its own.
        Object burndownWarnings = JsonPath.read(burndown, "$.warnings");
        Object burnupWarnings = JsonPath.read(burnup, "$.warnings");
        assertEquals(burndownWarnings, burnupWarnings);
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burndown_reopenedTask_remainingRisesAgain() throws Exception {
        doneTask.setIssueStatus(statusOf(IssueStatusID.IN_PROGRESS));
        issueRepository.save(doneTask);
        recordChange(sprint, doneTask, IssueStatusID.IN_PROGRESS, "2026-11-05T10:00:00Z");

        mvc.perform(get(BASE + "/" + sprint.getId() + "/burndown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[2].remaining", is(0)))
                .andExpect(jsonPath("$.points[3].remaining", is(5)))
                .andExpect(jsonPath("$.points[4].remaining", is(5)));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burndown_lastPointMatchesSummaryIncompleteEstimate() throws Exception {
        createIssue(sprint, 3, IssueStatusID.IN_PROGRESS);

        String burndown = bodyOf(sprint, "/burndown");
        String summary = bodyOf(sprint, "/summary");

        List<Map<String, Object>> points = JsonPath.read(burndown, "$.points");
        long lastRemaining = ((Number) points.get(points.size() - 1).get("remaining")).longValue();
        long incompleteEstimate = ((Number) JsonPath.read(summary, "$.incompleteEstimate")).longValue();

        assertEquals(3L, incompleteEstimate);
        assertEquals(incompleteEstimate, lastRemaining);
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burndownAndBurnup_areDeterministic() throws Exception {
        String burndownFirst = bodyOf(sprint, "/burndown");
        String burndownSecond = bodyOf(sprint, "/burndown");
        String burnupFirst = bodyOf(sprint, "/burnup");
        String burnupSecond = bodyOf(sprint, "/burnup");

        // Full JSON strings, not just the status: days, points and warnings must be stable.
        assertEquals(burndownFirst, burndownSecond);
        assertEquals(burnupFirst, burnupSecond);
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void burnup_nonNumericId_returns400WithApiError() throws Exception {
        mvc.perform(get(BASE + "/abc/burnup"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PARAMETER")))
                .andExpect(jsonPath("$.message", containsString("sprintId")));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    public void unestimatedSprint_scopeAndRemainingAreZeroWithWarning() throws Exception {
        Sprint unestimated = createSprint(project);
        createIssue(unestimated, null, IssueStatusID.IN_PROGRESS);

        mvc.perform(get(BASE + "/" + unestimated.getId() + "/burndown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(5)))
                .andExpect(jsonPath("$.points[*].remaining", everyItem(is(0))))
                .andExpect(jsonPath("$.warnings", hasItem(startsWith("UNESTIMATED_TASKS"))));
        mvc.perform(get(BASE + "/" + unestimated.getId() + "/burnup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(5)))
                .andExpect(jsonPath("$.points[*].scope", everyItem(is(0))))
                .andExpect(jsonPath("$.warnings", hasItem(startsWith("UNESTIMATED_TASKS"))));
    }

    private String bodyOf(Sprint target, String endpoint) throws Exception {
        return mvc.perform(get(BASE + "/" + target.getId() + endpoint))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private Sprint createSprint(Project project) {
        Sprint s = SprintBuilder.createSprintEntity(project, true);
        s.setStartDate(START.atStartOfDay(WARSAW));
        s.setEndDate(END.atTime(23, 59, 59).atZone(WARSAW));
        s.setSprintIssues(new HashSet<>());
        return sprintRepository.save(s);
    }

    private void coverHistoryFrom2020() {
        // Recording-start sentinel: the recording covers the sprint start, so the axis is the
        // whole planned 5-day range (rule E6).
        historyRepository.save(IssueStatusHistory.builder()
                .issueId(-1)
                .sprintId(null)
                .newStatus(statusOf(IssueStatusID.IN_PROGRESS))
                .changedAt(Instant.parse("2020-01-01T00:00:00Z"))
                .build());
    }

    private Issue createIssue(Sprint sprint, Integer estimation, IssueStatusID status) {
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setEstimation(estimation);
        issue.setIssueStatus(statusOf(status));
        return issueRepository.save(issue);
    }

    private void recordChange(Sprint sprint, Issue issue, IssueStatusID newStatus, String changedAt) {
        historyRepository.save(IssueStatusHistory.builder()
                .issueId(issue.getId())
                .sprintId(sprint.getId())
                .newStatus(statusOf(newStatus))
                .changedAt(Instant.parse(changedAt))
                .build());
    }

    private IssueStatus statusOf(IssueStatusID id) {
        return issueStatusRepository.findById(id.getStatusID()).orElseThrow();
    }
}
