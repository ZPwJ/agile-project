package io.github.student.agile.analytics.charts;

import io.github.student.agile.builder.SprintBuilder;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.analytics.charts.SprintChartDataCompleteness;
import io.github.student.agile.resource.analytics.charts.SprintTimeline;
import io.github.student.agile.resource.analytics.charts.SprintTimelineDay;
import io.github.student.agile.resource.analytics.charts.SprintTimelineService;
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
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithSprint;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Timeline rules of plan section 3.2-3.5: inclusive Warsaw day axis and points finished per day.
 * Rows are written with explicit instants so no clock or time zone of the machine leaks in.
 */
@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class SprintTimelineServiceTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static final LocalDate END = LocalDate.of(2026, 11, 6);

    @Autowired
    private SprintTimelineService timelineService;
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

    @BeforeEach
    public void setUp() {
        project = projectRepository.save(createProjectEntity());
        Backlog backlog = project.getBacklog();
        backlog.setProject(project);
        backlogRepository.save(backlog);
        // Recording-start sentinel: an old row of another sprint makes the recording cover every
        // sprint under test, so the axis always starts at the sprint start (E6 is covered in B4).
        historyRepository.save(IssueStatusHistory.builder()
                .issueId(-1)
                .sprintId(null)
                .newStatus(status(IssueStatusID.IN_PROGRESS))
                .changedAt(Instant.parse("2020-01-01T00:00:00Z"))
                .build());
    }

    @Test
    public void axis_coversFirstAndLastDayInclusive() {
        Sprint sprint = sprint();
        issue(sprint, 5, IssueStatusID.IN_PROGRESS);

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertEquals(5, timeline.days().size());
        assertEquals(List.of(
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 3),
                LocalDate.of(2026, 11, 4),
                LocalDate.of(2026, 11, 5),
                LocalDate.of(2026, 11, 6)), timeline.days().stream().map(SprintTimelineDay::date).toList());
        assertEquals(5, timeline.initialScope());
    }

    @Test
    public void completedAndCumulativeComeFromHistoryRows() {
        Sprint sprint = sprint();
        Issue first = issue(sprint, 5, IssueStatusID.DONE);
        Issue second = issue(sprint, 3, IssueStatusID.DONE);
        change(sprint, first, IssueStatusID.DONE, "2026-11-03T10:00:00Z");
        change(sprint, second, IssueStatusID.DONE, "2026-11-04T10:00:00Z");

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertDay(timeline, "2026-11-02", 0, 0);
        assertDay(timeline, "2026-11-03", 5, 5);
        assertDay(timeline, "2026-11-04", 3, 8);
        assertDay(timeline, "2026-11-05", 0, 8);
        assertDay(timeline, "2026-11-06", 0, 8);
        assertEquals(8, timeline.initialScope());
        assertEquals(SprintChartDataCompleteness.COMPLETE, timeline.dataCompleteness());
    }

    @Test
    public void historyRowsOfAnotherSprintDoNotAffectTheTimeline() {
        Sprint current = sprint();
        Sprint other = sprint();
        Issue currentTask = issue(current, 5, IssueStatusID.DONE);
        Issue otherTask = issue(other, 8, IssueStatusID.DONE);
        change(current, currentTask, IssueStatusID.DONE, "2026-11-03T10:00:00Z");
        change(other, otherTask, IssueStatusID.DONE, "2026-11-02T10:00:00Z");

        SprintTimeline timeline = timelineService.forSprint(current.getId());

        assertDay(timeline, "2026-11-02", 0, 0);
        assertDay(timeline, "2026-11-03", 5, 5);
        assertDay(timeline, "2026-11-04", 0, 5);
        assertDay(timeline, "2026-11-05", 0, 5);
        assertDay(timeline, "2026-11-06", 0, 5);
        assertEquals(5, timeline.initialScope());
    }

    @Test
    public void changeAt2330UtcCountsOnTheNextWarsawDay() {
        Sprint sprint = sprint();
        Issue task = issue(sprint, 5, IssueStatusID.DONE);
        // 2026-11-03T23:30Z is 2026-11-04T00:30 in Europe/Warsaw (CET, UTC+1).
        change(sprint, task, IssueStatusID.DONE, "2026-11-03T23:30:00Z");

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertDay(timeline, "2026-11-02", 0, 0);
        assertDay(timeline, "2026-11-03", 0, 0);
        assertDay(timeline, "2026-11-04", 5, 5);
        assertDay(timeline, "2026-11-05", 0, 5);
        assertDay(timeline, "2026-11-06", 0, 5);
    }

    @Test
    public void reopenedTaskLosesItsPointsFromTheReopenDay() {
        Sprint sprint = sprint();
        Issue task = issue(sprint, 5, IssueStatusID.IN_PROGRESS);
        change(sprint, task, IssueStatusID.DONE, "2026-11-03T10:00:00Z");
        change(sprint, task, IssueStatusID.IN_PROGRESS, "2026-11-05T10:00:00Z");

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertDay(timeline, "2026-11-02", 0, 0);
        assertDay(timeline, "2026-11-03", 5, 5);
        assertDay(timeline, "2026-11-04", 0, 5);
        // E11: the reopen is a change on its own day, so this day's completed points are negative.
        assertDay(timeline, "2026-11-05", -5, 0);
        assertDay(timeline, "2026-11-06", 0, 0);
        assertTrue(day(timeline, "2026-11-05").cumulativeCompleted()
                        < day(timeline, "2026-11-04").cumulativeCompleted(),
                "cumulative completed must drop on the reopen day");
    }

    private Sprint sprint() {
        Sprint sprint = SprintBuilder.createSprintEntity(project, Boolean.TRUE);
        sprint.setStartDate(START.atStartOfDay(WARSAW));
        sprint.setEndDate(END.atTime(23, 59, 59).atZone(WARSAW));
        sprint.setIsDone(Boolean.FALSE);
        sprint.setSprintIssues(new HashSet<>());
        return sprintRepository.save(sprint);
    }

    private Issue issue(Sprint sprint, Integer estimation, IssueStatusID status) {
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setEstimation(estimation);
        issue.setIssueStatus(status(status));
        Issue saved = issueRepository.save(issue);
        sprint.getSprintIssues().add(saved);
        return saved;
    }

    private void change(Sprint sprint, Issue issue, IssueStatusID newStatus, String changedAt) {
        historyRepository.save(IssueStatusHistory.builder()
                .issueId(issue.getId())
                .sprintId(sprint.getId())
                .newStatus(status(newStatus))
                .changedAt(Instant.parse(changedAt))
                .build());
    }

    private IssueStatus status(IssueStatusID id) {
        return issueStatusRepository.findById(id.getStatusID()).orElseThrow();
    }

    private static void assertDay(SprintTimeline timeline, String date, long completed, long cumulative) {
        SprintTimelineDay day = day(timeline, date);
        assertEquals(completed, day.completed(), "completed on " + date);
        assertEquals(cumulative, day.cumulativeCompleted(), "cumulativeCompleted on " + date);
    }

    private static SprintTimelineDay day(SprintTimeline timeline, String date) {
        LocalDate localDate = LocalDate.parse(date);
        return timeline.days().stream()
                .filter(day -> day.date().equals(localDate))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no timeline point for " + date));
    }
}
