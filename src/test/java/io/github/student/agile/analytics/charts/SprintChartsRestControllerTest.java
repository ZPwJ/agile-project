package io.github.student.agile.analytics.charts;

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

import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithSprint;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WF-3 burndown endpoint: JSON shape of plan section 2.1, ascending non-negative points,
 * and the existing analytics error body for 404/400.
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

    private Sprint sprint;

    @BeforeEach
    void setUp() {
        Project project = projectRepository.save(createProjectEntity());
        Backlog backlog = project.getBacklog();
        backlog.setProject(project);
        backlogRepository.save(backlog);

        Sprint s = SprintBuilder.createSprintEntity(project, true);
        s.setStartDate(START.atStartOfDay(WARSAW));
        s.setEndDate(END.atTime(23, 59, 59).atZone(WARSAW));
        s.setSprintIssues(new HashSet<>());
        sprint = sprintRepository.save(s);

        // Recording-start sentinel: the recording covers the sprint start, so the axis is the
        // whole planned 5-day range (rule E6).
        historyRepository.save(IssueStatusHistory.builder()
                .issueId(-1)
                .sprintId(null)
                .newStatus(statusOf(IssueStatusID.IN_PROGRESS))
                .changedAt(Instant.parse("2020-01-01T00:00:00Z"))
                .build());

        Issue done = createIssueEntityWithSprint(sprint);
        done.setEstimation(5);
        done.setIssueStatus(statusOf(IssueStatusID.DONE));
        issueRepository.save(done);

        // The only work of the sprint is finished on the third day of the axis.
        historyRepository.save(IssueStatusHistory.builder()
                .issueId(done.getId())
                .sprintId(sprint.getId())
                .newStatus(statusOf(IssueStatusID.DONE))
                .changedAt(Instant.parse("2026-11-04T10:00:00Z"))
                .build());
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

    private IssueStatus statusOf(IssueStatusID id) {
        return issueStatusRepository.findById(id.getStatusID()).orElseThrow();
    }
}
