package io.github.student.agile.analytics.charts;

import io.github.student.agile.builder.SprintBuilder;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.analytics.charts.SprintChartDataCompleteness;
import io.github.student.agile.resource.analytics.charts.SprintChartWarning;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Timeline rules of plan sections 3.2-3.6: inclusive Warsaw day axis, points finished per day,
 * and every edge rule E1-E12 of section 3.4. Rows are written with explicit instants so no clock
 * or time zone of the machine leaks in.
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
        // sprint under test, so the axis always starts at the sprint start. Tests that need a
        // different recording start call clearHistory() and control it explicitly.
        coverHistoryFrom("2020-01-01T00:00:00Z");
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

    // RULE E1: a sprint without a start date gets no points and the NO_DATES warning.
    @Test
    public void noStartDate_noPointsAndNoDates() {
        Sprint sprint = sprint();
        sprint.setStartDate(null);
        sprintRepository.save(sprint);
        issue(sprint, 5, IssueStatusID.DONE);

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertTrue(timeline.days().isEmpty());
        assertEquals(5, timeline.initialScope());
        assertEquals(SprintChartDataCompleteness.PARTIAL, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.NO_DATES);
    }

    // RULE E2: an end date before the start date is invalid data.
    @Test
    public void endBeforeStart_noPointsAndInvalidDateRange() {
        Sprint sprint = sprint(false, false);
        sprint.setStartDate(LocalDate.of(2026, 11, 6).atStartOfDay(WARSAW));
        sprint.setEndDate(LocalDate.of(2026, 11, 2).atTime(23, 59, 59).atZone(WARSAW));
        sprintRepository.save(sprint);
        issue(sprint, 5, IssueStatusID.IN_PROGRESS);

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertTrue(timeline.days().isEmpty());
        assertEquals(5, timeline.initialScope());
        assertEquals(SprintChartDataCompleteness.PARTIAL, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.INVALID_DATE_RANGE);
    }

    // RULE E3: no recorded change and no coverage of the sprint start means no points at all.
    @Test
    public void noRecordedChanges_axisNotCovered_noPointsAndHistoryStartedLate() {
        clearHistory();
        Sprint sprint = sprint();
        issue(sprint, 5, IssueStatusID.IN_PROGRESS);

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertTrue(timeline.days().isEmpty());
        assertEquals(SprintChartDataCompleteness.PARTIAL, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.NO_RECORDED_CHANGES);
        assertHasWarning(timeline, SprintChartWarning.HISTORY_STARTED_LATE);
    }

    // RULE E3: no recorded change but full coverage means a flat line at the scope.
    @Test
    public void noRecordedChanges_historyCovered_flatLineAtScope() {
        Sprint sprint = sprint();
        issue(sprint, 5, IssueStatusID.IN_PROGRESS);

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertEquals(5, timeline.initialScope());
        assertEquals(5, timeline.days().size());
        assertEquals(List.of(0L, 0L, 0L, 0L, 0L),
                timeline.days().stream().map(SprintTimelineDay::cumulativeCompleted).toList());
        assertEquals(SprintChartDataCompleteness.COMPLETE, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.NO_RECORDED_CHANGES);
        assertNoWarning(timeline, SprintChartWarning.HISTORY_STARTED_LATE);
        // RULE E12: points are returned, so the scope is flagged as not historical.
        assertHasWarning(timeline, SprintChartWarning.SCOPE_NOT_HISTORICAL);
    }

    // RULE E4: an active sprint without an end date ends today, never in the future.
    @Test
    public void activeSprintWithoutEndDate_lastDayIsToday() {
        LocalDate today = LocalDate.now(WARSAW);
        Sprint sprint = sprint();
        sprint.setStartDate(today.minusDays(2).atStartOfDay(WARSAW));
        sprint.setEndDate(null);
        sprintRepository.save(sprint);
        Issue task = issue(sprint, 5, IssueStatusID.DONE);
        change(sprint, task, IssueStatusID.DONE, instantAt(today.minusDays(1), 10));

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertEquals(3, timeline.days().size());
        assertEquals(today, timeline.days().get(2).date());
        assertEquals(SprintChartDataCompleteness.COMPLETE, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.SPRINT_IN_PROGRESS);
        assertNoWarning(timeline, SprintChartWarning.NO_RECORDED_CHANGES);
    }

    // RULE E5: a planned sprint is shown for its planned days only, a single day without an end.
    @Test
    public void plannedSprint_notStartedWarning() {
        Sprint sprint = sprint(false, false);
        issue(sprint, 5, IssueStatusID.IN_PROGRESS);

        SprintTimeline withEnd = timelineService.forSprint(sprint.getId());

        assertEquals(5, withEnd.days().size());
        assertEquals(SprintChartDataCompleteness.COMPLETE, withEnd.dataCompleteness());
        assertHasWarning(withEnd, SprintChartWarning.SPRINT_NOT_STARTED);

        sprint.setEndDate(null);
        sprintRepository.save(sprint);
        SprintTimeline withoutEnd = timelineService.forSprint(sprint.getId());

        assertEquals(1, withoutEnd.days().size());
        assertEquals(START, withoutEnd.days().get(0).date());
    }

    // RULE E6: when recording starts late, points may only start at the first recorded change.
    @Test
    public void recordingStartedLate_pointsStartAtFirstRecordedChange() {
        clearHistory();
        coverHistoryFrom(instantAt(START.plusDays(1), 8));
        Sprint sprint = sprint();
        Issue task = issue(sprint, 5, IssueStatusID.DONE);
        change(sprint, task, IssueStatusID.DONE, instantAt(START.plusDays(2), 10));

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertEquals(3, timeline.days().size());
        assertEquals(LocalDate.of(2026, 11, 4), timeline.days().get(0).date());
        assertDay(timeline, "2026-11-04", 5, 5);
        assertDay(timeline, "2026-11-05", 0, 5);
        assertDay(timeline, "2026-11-06", 0, 5);
        assertEquals(SprintChartDataCompleteness.PARTIAL, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.HISTORY_STARTED_LATE);
        assertNoWarning(timeline, SprintChartWarning.NO_RECORDED_CHANGES);
    }

    // RULE E7: changes of tasks outside the current scope are ignored and counted in the warning.
    @Test
    public void changeOfTaskOutsideScope_ignoredAndWarned() {
        Sprint sprint = sprint();
        issue(sprint, 5, IssueStatusID.IN_PROGRESS);
        change(sprint, -999, IssueStatusID.DONE, instantAt(START.plusDays(1), 10));

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertEquals(5, timeline.days().size());
        assertEquals(List.of(0L, 0L, 0L, 0L, 0L),
                timeline.days().stream().map(SprintTimelineDay::cumulativeCompleted).toList());
        assertEquals(SprintChartDataCompleteness.COMPLETE, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.SCOPE_CHANGED, 1);
    }

    // RULE E8: a task completed in the scope source without a dated DONE change is not counted.
    @Test
    public void completedTaskWithoutRecordedDoneChange_undatedCompletion() {
        Sprint sprint = sprint();
        issue(sprint, 5, IssueStatusID.DONE);
        Issue open = issue(sprint, 3, IssueStatusID.IN_PROGRESS);
        change(sprint, open, IssueStatusID.IN_PROGRESS, instantAt(START.plusDays(1), 10));

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertEquals(8, timeline.initialScope());
        assertEquals(5, timeline.days().size());
        assertEquals(List.of(0L, 0L, 0L, 0L, 0L),
                timeline.days().stream().map(SprintTimelineDay::cumulativeCompleted).toList());
        assertEquals(SprintChartDataCompleteness.PARTIAL, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.UNDATED_COMPLETION, 1);
    }

    // RULE E9: a task without estimation is still in scope but adds 0 points.
    @Test
    public void unestimatedTask_countsZeroPoints() {
        Sprint sprint = sprint();
        Issue task = issue(sprint, null, IssueStatusID.DONE);
        change(sprint, task, IssueStatusID.DONE, instantAt(START.plusDays(1), 10));

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertEquals(0, timeline.initialScope());
        assertEquals(5, timeline.days().size());
        assertEquals(List.of(0L, 0L, 0L, 0L, 0L),
                timeline.days().stream().map(SprintTimelineDay::cumulativeCompleted).toList());
        assertEquals(SprintChartDataCompleteness.COMPLETE, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.UNESTIMATED_TASKS, 1);
    }

    // RULE E10: a sprint with no tasks is reported explicitly.
    @Test
    public void emptySprint_emptySprintWarning() {
        Sprint sprint = sprint();

        SprintTimeline timeline = timelineService.forSprint(sprint.getId());

        assertEquals(0, timeline.initialScope());
        assertEquals(5, timeline.days().size());
        assertEquals(SprintChartDataCompleteness.COMPLETE, timeline.dataCompleteness());
        assertHasWarning(timeline, SprintChartWarning.EMPTY_SPRINT);
    }

    private Sprint sprint() {
        return sprint(true, false);
    }

    private Sprint sprint(boolean active, boolean done) {
        Sprint sprint = SprintBuilder.createSprintEntity(project, active);
        sprint.setIsDone(done);
        sprint.setStartDate(START.atStartOfDay(WARSAW));
        sprint.setEndDate(END.atTime(23, 59, 59).atZone(WARSAW));
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
        change(sprint, issue.getId(), newStatus, changedAt);
    }

    private void change(Sprint sprint, Integer issueId, IssueStatusID newStatus, String changedAt) {
        historyRepository.save(IssueStatusHistory.builder()
                .issueId(issueId)
                .sprintId(sprint.getId())
                .newStatus(status(newStatus))
                .changedAt(Instant.parse(changedAt))
                .build());
    }

    private void coverHistoryFrom(String changedAt) {
        historyRepository.save(IssueStatusHistory.builder()
                .issueId(-1)
                .sprintId(null)
                .newStatus(status(IssueStatusID.IN_PROGRESS))
                .changedAt(Instant.parse(changedAt))
                .build());
    }

    private void clearHistory() {
        historyRepository.deleteAll();
        historyRepository.flush();
    }

    private static String instantAt(LocalDate date, int hour) {
        return date.atTime(hour, 0).atZone(WARSAW).toInstant().toString();
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

    private static void assertHasWarning(SprintTimeline timeline, SprintChartWarning warning, Object... args) {
        String expected = warning.render(args);
        assertTrue(timeline.warnings().contains(expected),
                () -> "expected warning " + expected + " in " + timeline.warnings());
    }

    private static void assertNoWarning(SprintTimeline timeline, SprintChartWarning warning) {
        String code = warning.name() + ":";
        assertFalse(timeline.warnings().stream().anyMatch(message -> message.startsWith(code)),
                () -> "unexpected warning " + warning + " in " + timeline.warnings());
    }
}
