package io.github.student.agile.service;

import io.github.student.agile.exception.definition.NullIdentifierException;
import io.github.student.agile.exception.definition.ProjectAccessDeniedException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.domain.dictionary.SprintEventType;
import io.github.student.agile.resource.dto.BurndownChartDTO;
import io.github.student.agile.resource.dto.BurndownPointDTO;
import io.github.student.agile.resource.dto.BurnupChartDTO;
import io.github.student.agile.resource.dto.BurnupPointDTO;
import io.github.student.agile.resource.dto.SprintComparisonItemDTO;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.SprintAnalyticsServiceImpl;
import io.github.student.agile.resource.service.SprintVelocityCalculator;
import io.github.student.agile.resource.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTION_NOT_ALLOWED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.student.agile.exception.definition.InvalidDateRangeException;
import io.github.student.agile.resource.dto.SprintComparisonItemDTO;   // już jest
import static io.github.student.agile.exception.dictionary.ExceptionMessages.INVALID_DATE_RANGE;
import static org.mockito.ArgumentMatchers.anyCollection;

@ExtendWith(MockitoExtension.class)
public class SprintAnalyticsServiceImplTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private SprintIssueEventRepository sprintIssueEventRepository;

    @Mock
    private UserService userService;

    private SprintAnalyticsServiceImpl sprintAnalyticsService;

    @BeforeEach
    void setUp() {
        sprintAnalyticsService = new SprintAnalyticsServiceImpl(
                sprintRepository,
                issueRepository,
                sprintIssueEventRepository,
                new SprintVelocityCalculator(),
                userService
        );
    }

    @Test
    public void getSprintSummaryReport_completedSprintReconstructsUncompletedIssuesFromEvents() {
        Integer projectId = 10;
        Integer sprintId = 100;
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime endDate = ZonedDateTime.of(2026, 11, 15, 17, 0, 0, 0, ZoneOffset.UTC);

        Project project = Project.builder().id(projectId).build();
        Sprint sprint = Sprint.builder()
                .id(sprintId)
                .name("Sprint 1")
                .project(project)
                .startDate(startDate)
                .endDate(endDate)
                .active(false)
                .isDone(true)
                .build();

        // After stopSprint, only DONE issues remain linked to sprint
        Issue doneIssue = Issue.builder()
                .id(1)
                .sprint(sprint)
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(5)
                .build();
        Issue doneWithoutEstimation = Issue.builder()
                .id(2)
                .sprint(sprint)
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(null)
                .build();

        List<SprintIssueEvent> events = List.of(
                event(sprintId, 1, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 5, startDate),
                event(sprintId, 2, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), null, startDate),
                event(sprintId, 3, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 3, startDate),
                event(sprintId, 4, SprintEventType.ADDED_TO_SPRINT, IssueStatusID.TO_DO.getStatusID(), 8, startDate.plusDays(1)),
                event(sprintId, 3, SprintEventType.REMOVED_FROM_SPRINT, IssueStatusID.TO_DO.getStatusID(), 3, startDate.plusDays(2)),
                event(sprintId, 1, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), 5, startDate.plusDays(3)),
                event(sprintId, 2, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), null, startDate.plusDays(4)),
                event(sprintId, 4, SprintEventType.ESTIMATION_CHANGED, IssueStatusID.IN_PROGRESS.getStatusID(), 13, startDate.plusDays(5)),
                event(sprintId, null, SprintEventType.SPRINT_STOPPED, null, null, endDate)
        );

        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(issueRepository.findAllBySprintId(sprintId)).thenReturn(List.of(doneIssue, doneWithoutEstimation));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId)).thenReturn(events);

        SprintSummaryReportDTO report = sprintAnalyticsService.getSprintSummaryReport(sprintId);

        verify(userService).verifyProjectAccess(projectId);
        assertEquals(sprintId, report.getSprintId());
        assertEquals("Sprint 1", report.getSprintName());
        assertEquals(projectId, report.getProjectId());
        assertEquals(startDate, report.getStartDate());
        assertEquals(endDate, report.getEndDate());
        assertEquals(3, report.getInitialIssueCount());
        assertEquals(2, report.getCompletedIssueCount());
        assertEquals(1, report.getUncompletedIssueCount());
        assertEquals(5, report.getVelocity());
        assertEquals(13, report.getUncompletedEstimationSum());
        assertEquals("STORY_POINTS", report.getEstimationUnit());
    }

    @Test
    public void getSprintSummaryReport_activeSprintAllowsNullEndDateAndNullEstimations() {
        Integer projectId = 11;
        Integer sprintId = 101;
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC);

        Project project = Project.builder().id(projectId).build();
        Sprint activeSprint = Sprint.builder()
                .id(sprintId)
                .name("Active Sprint")
                .project(project)
                .startDate(startDate)
                .endDate(null)
                .active(true)
                .isDone(false)
                .build();

        Issue doneWithEstimation = Issue.builder()
                .id(10)
                .sprint(activeSprint)
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(8)
                .build();
        Issue inProgressWithoutEstimation = Issue.builder()
                .id(11)
                .sprint(activeSprint)
                .issueStatus(new IssueStatus(IssueStatusID.IN_PROGRESS.getStatusID(), null))
                .estimation(null)
                .build();
        Issue toDoWithEstimation = Issue.builder()
                .id(12)
                .sprint(activeSprint)
                .issueStatus(new IssueStatus(IssueStatusID.TO_DO.getStatusID(), null))
                .estimation(5)
                .build();

        List<SprintIssueEvent> events = List.of(
                event(sprintId, 10, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 8, startDate),
                event(sprintId, 11, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), null, startDate),
                event(sprintId, 12, SprintEventType.ADDED_TO_SPRINT, IssueStatusID.TO_DO.getStatusID(), 5, startDate.plusDays(1))
        );

        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(activeSprint));
        when(issueRepository.findAllBySprintId(sprintId))
                .thenReturn(List.of(doneWithEstimation, inProgressWithoutEstimation, toDoWithEstimation));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId)).thenReturn(events);

        SprintSummaryReportDTO report = sprintAnalyticsService.getSprintSummaryReport(sprintId);

        assertNull(report.getEndDate());
        assertEquals(2, report.getInitialIssueCount());
        assertEquals(1, report.getCompletedIssueCount());
        assertEquals(2, report.getUncompletedIssueCount());
        assertEquals(8, report.getVelocity());
        assertEquals(5, report.getUncompletedEstimationSum());
        assertEquals("STORY_POINTS", report.getEstimationUnit());
    }

    @Test
    public void getSprintSummaryReport_withoutEventHistoryFallsBackToCurrentIssues() {
        Project project = Project.builder().id(12).build();
        Sprint sprint = Sprint.builder()
                .id(102)
                .name("Legacy Sprint")
                .project(project)
                .isDone(true)
                .build();

        Issue doneIssue = Issue.builder()
                .id(20)
                .sprint(sprint)
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(null)
                .build();
        Issue uncompletedIssue = Issue.builder()
                .id(21)
                .sprint(sprint)
                .issueStatus(new IssueStatus(IssueStatusID.TO_DO.getStatusID(), null))
                .estimation(null)
                .build();

        when(sprintRepository.findById(102)).thenReturn(Optional.of(sprint));
        when(issueRepository.findAllBySprintId(102)).thenReturn(List.of(doneIssue, uncompletedIssue));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(102))
                .thenReturn(Collections.emptyList());

        SprintSummaryReportDTO report = sprintAnalyticsService.getSprintSummaryReport(102);

        assertEquals(2, report.getInitialIssueCount());
        assertEquals(1, report.getCompletedIssueCount());
        assertEquals(1, report.getUncompletedIssueCount());
        assertEquals(0, report.getVelocity());
        assertEquals(0, report.getUncompletedEstimationSum());
    }

    @Test
    public void getSprintSummaryReport_throwsResourceNotFoundWhenSprintDoesNotExist() {
        when(sprintRepository.findById(999)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> sprintAnalyticsService.getSprintSummaryReport(999)
        );
        assertEquals(SPRINT_DOES_NOT_EXISTS, exception.getMessage());
        verifyNoInteractions(userService, issueRepository, sprintIssueEventRepository);
    }

    @Test
    public void getSprintSummaryReport_throwsProjectAccessDeniedWhenUserHasNoAccess() {
        Project project = Project.builder().id(50).build();
        Sprint sprint = Sprint.builder().id(105).project(project).build();

        when(sprintRepository.findById(105)).thenReturn(Optional.of(sprint));
        doThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED))
                .when(userService).verifyProjectAccess(50);

        ProjectAccessDeniedException exception = assertThrows(
                ProjectAccessDeniedException.class,
                () -> sprintAnalyticsService.getSprintSummaryReport(105)
        );
        assertEquals(ACTION_NOT_ALLOWED, exception.getMessage());
        verifyNoInteractions(issueRepository, sprintIssueEventRepository);
    }

    @Test
    public void getSprintSummaryReport_throwsNullIdentifierWhenSprintIdIsNull() {
        assertThrows(NullIdentifierException.class, () -> sprintAnalyticsService.getSprintSummaryReport(null));
        verifyNoInteractions(sprintRepository, userService, issueRepository, sprintIssueEventRepository);
    }

    @Test
    public void getBurnupChart_tracksChronologicalCompletedWorkAndScopeChanges() {
        Integer projectId = 10;
        Integer sprintId = 200;
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime endDate = ZonedDateTime.of(2026, 11, 5, 17, 0, 0, 0, ZoneOffset.UTC);

        Project project = Project.builder().id(projectId).build();
        Sprint sprint = Sprint.builder()
                .id(sprintId)
                .name("Burnup Sprint")
                .project(project)
                .startDate(startDate)
                .endDate(endDate)
                .isDone(true)
                .build();

        // Day 1 (Nov 1): issue 1 (5 pts, TO_DO), issue 2 (3 pts, TO_DO) -> total=8, completed=0
        // Day 2 (Nov 2): issue 3 added (8 pts, TO_DO), issue 1 -> DONE -> total=16, completed=5
        // Day 3 (Nov 3): issue 2 removed (-3 pts) -> total=13, completed=5
        // Day 4 (Nov 4): issue 3 estimation 8 -> 13 -> total=18, completed=5
        // Day 5 (Nov 5): issue 3 -> DONE, SPRINT_STOPPED -> total=18, completed=18
        List<SprintIssueEvent> events = List.of(
                event(sprintId, 1, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 5, startDate),
                event(sprintId, 2, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 3, startDate),
                event(sprintId, 3, SprintEventType.ADDED_TO_SPRINT, IssueStatusID.TO_DO.getStatusID(), 8, startDate.plusDays(1)),
                event(sprintId, 1, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), 5, startDate.plusDays(1).plusHours(3)),
                event(sprintId, 2, SprintEventType.REMOVED_FROM_SPRINT, IssueStatusID.TO_DO.getStatusID(), 3, startDate.plusDays(2)),
                event(sprintId, 3, SprintEventType.ESTIMATION_CHANGED, IssueStatusID.IN_PROGRESS.getStatusID(), 13, startDate.plusDays(3)),
                event(sprintId, 3, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), 13, startDate.plusDays(4)),
                event(sprintId, null, SprintEventType.SPRINT_STOPPED, null, null, endDate)
        );

        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId)).thenReturn(events);

        BurnupChartDTO chart = sprintAnalyticsService.getBurnupChart(sprintId);

        verify(userService).verifyProjectAccess(projectId);
        assertEquals(sprintId, chart.getSprintId());
        assertEquals("Burnup Sprint", chart.getSprintName());
        assertEquals(projectId, chart.getProjectId());
        assertEquals(startDate, chart.getStartDate());
        assertEquals(endDate, chart.getEndDate());
        assertEquals("STORY_POINTS", chart.getUnit());
        assertEquals(5, chart.getPoints().size());

        assertPoint(chart.getPoints().get(0), LocalDate.of(2026, 11, 1), 0, 8);
        assertPoint(chart.getPoints().get(1), LocalDate.of(2026, 11, 2), 5, 16);
        assertPoint(chart.getPoints().get(2), LocalDate.of(2026, 11, 3), 5, 13);
        assertPoint(chart.getPoints().get(3), LocalDate.of(2026, 11, 4), 5, 18);
        assertPoint(chart.getPoints().get(4), LocalDate.of(2026, 11, 5), 18, 18);
    }

    @Test
    public void getBurnupChart_handlesNullEstimationsMultipleSameDayUpdatesAndDaysWithoutEvents() {
        Integer sprintId = 201;
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime endDate = ZonedDateTime.of(2026, 11, 3, 17, 0, 0, 0, ZoneOffset.UTC);
        Sprint sprint = Sprint.builder()
                .id(sprintId)
                .name("Null Estimation Sprint")
                .project(Project.builder().id(10).build())
                .startDate(startDate)
                .endDate(endDate)
                .isDone(true)
                .build();

        List<SprintIssueEvent> events = List.of(
                event(sprintId, 1, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), null, startDate),
                event(sprintId, 2, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 5, startDate),
                event(sprintId, 1, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), null, startDate.plusHours(2)),
                event(sprintId, 2, SprintEventType.STATUS_CHANGED, IssueStatusID.IN_PROGRESS.getStatusID(), 5, startDate.plusHours(3)),
                event(sprintId, 2, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), 5, startDate.plusDays(2)),
                event(sprintId, null, SprintEventType.SPRINT_STOPPED, null, null, endDate)
        );

        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId)).thenReturn(events);

        BurnupChartDTO chart = sprintAnalyticsService.getBurnupChart(sprintId);

        assertEquals(3, chart.getPoints().size());
        assertPoint(chart.getPoints().get(0), LocalDate.of(2026, 11, 1), 0, 5);
        assertPoint(chart.getPoints().get(1), LocalDate.of(2026, 11, 2), 0, 5);
        assertPoint(chart.getPoints().get(2), LocalDate.of(2026, 11, 3), 5, 5);
    }

    @Test
    public void getBurnupChart_isolatesSprintEventsAndFallbackIssuesToTargetSprint() {
        Integer targetSprintId = 202;
        Integer otherSprintId = 999;
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime endDate = ZonedDateTime.of(2026, 11, 2, 17, 0, 0, 0, ZoneOffset.UTC);

        Sprint sprint = Sprint.builder()
                .id(targetSprintId)
                .name("Isolated Sprint")
                .project(Project.builder().id(10).build())
                .startDate(startDate)
                .endDate(endDate)
                .isDone(true)
                .build();

        List<SprintIssueEvent> events = List.of(
                event(targetSprintId, 1, SprintEventType.SPRINT_STARTED, IssueStatusID.DONE.getStatusID(), 5, startDate),
                event(otherSprintId, 99, SprintEventType.SPRINT_STARTED, IssueStatusID.DONE.getStatusID(), 100, startDate),
                event(targetSprintId, null, SprintEventType.SPRINT_STOPPED, null, null, endDate)
        );

        when(sprintRepository.findById(targetSprintId)).thenReturn(Optional.of(sprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(targetSprintId)).thenReturn(events);

        BurnupChartDTO chart = sprintAnalyticsService.getBurnupChart(targetSprintId);

        verify(sprintIssueEventRepository).findAllBySprintIdOrderByOccurredAtAscIdAsc(targetSprintId);
        assertEquals(2, chart.getPoints().size());
        assertPoint(chart.getPoints().get(0), LocalDate.of(2026, 11, 1), 5, 5);
        assertPoint(chart.getPoints().get(1), LocalDate.of(2026, 11, 2), 5, 5);
    }

    @Test
    public void getBurnupChart_activeSprintWithoutEndDateBuildsPointsThroughAtLeastStartDay() {
        Integer sprintId = 203;
        ZonedDateTime startDate = ZonedDateTime.now(ZoneOffset.UTC).minusDays(2);
        Sprint activeSprint = Sprint.builder()
                .id(sprintId)
                .name("Active Burnup")
                .project(Project.builder().id(10).build())
                .startDate(startDate)
                .endDate(null)
                .active(true)
                .isDone(false)
                .build();

        List<SprintIssueEvent> events = List.of(
                event(sprintId, 1, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 8, startDate),
                event(sprintId, 1, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), 8, startDate.plusDays(1))
        );

        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(activeSprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId)).thenReturn(events);

        BurnupChartDTO chart = sprintAnalyticsService.getBurnupChart(sprintId);

        assertNull(chart.getEndDate());
        assertFalse(chart.getPoints().isEmpty());
        assertEquals(3, chart.getPoints().size());
        assertPoint(chart.getPoints().get(0), startDate.toLocalDate(), 0, 8);
        assertPoint(chart.getPoints().get(1), startDate.toLocalDate().plusDays(1), 8, 8);
        assertPoint(chart.getPoints().get(2), startDate.toLocalDate().plusDays(2), 8, 8);
    }

    @Test
    public void getBurnupChart_handlesLegacySprintWithoutEventsAndUnstartedSprint() {
        Integer legacySprintId = 204;
        Integer unstartedSprintId = 205;
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);

        Sprint legacySprint = Sprint.builder()
                .id(legacySprintId)
                .name("Legacy Sprint")
                .project(Project.builder().id(10).build())
                .startDate(startDate)
                .build();
        Sprint unstartedSprint = Sprint.builder()
                .id(unstartedSprintId)
                .name("Planned Sprint")
                .project(Project.builder().id(10).build())
                .startDate(null)
                .build();

        Issue doneIssue = Issue.builder()
                .id(1)
                .sprint(legacySprint)
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(5)
                .build();
        Issue todoIssue = Issue.builder()
                .id(2)
                .sprint(legacySprint)
                .issueStatus(new IssueStatus(IssueStatusID.TO_DO.getStatusID(), null))
                .estimation(3)
                .build();
        Issue otherSprintIssue = Issue.builder()
                .id(3)
                .sprint(unstartedSprint)
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(99)
                .build();

        when(sprintRepository.findById(legacySprintId)).thenReturn(Optional.of(legacySprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(legacySprintId))
                .thenReturn(Collections.emptyList());
        when(issueRepository.findAllBySprintId(legacySprintId))
                .thenReturn(List.of(doneIssue, todoIssue, otherSprintIssue));

        when(sprintRepository.findById(unstartedSprintId)).thenReturn(Optional.of(unstartedSprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(unstartedSprintId))
                .thenReturn(Collections.emptyList());

        BurnupChartDTO legacyChart = sprintAnalyticsService.getBurnupChart(legacySprintId);
        BurnupChartDTO unstartedChart = sprintAnalyticsService.getBurnupChart(unstartedSprintId);

        assertEquals(1, legacyChart.getPoints().size());
        assertPoint(legacyChart.getPoints().get(0), LocalDate.of(2026, 11, 1), 5, 8);
        assertTrue(unstartedChart.getPoints().isEmpty());
    }

    @Test
    public void getBurnupChart_throwsOnNullSprintIdMissingSprintAndUnauthorizedAccess() {
        assertThrows(NullIdentifierException.class, () -> sprintAnalyticsService.getBurnupChart(null));

        when(sprintRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> sprintAnalyticsService.getBurnupChart(999));

        Sprint unauthorizedSprint = Sprint.builder().id(206).project(Project.builder().id(50).build()).build();
        when(sprintRepository.findById(206)).thenReturn(Optional.of(unauthorizedSprint));
        doThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED)).when(userService).verifyProjectAccess(50);

        assertThrows(ProjectAccessDeniedException.class, () -> sprintAnalyticsService.getBurnupChart(206));
    }

    @Test
    public void getProjectSprintsComparison_returnsSortedSprintsIncludingZeroVelocityAndExcludesPlannedDrafts() {
        Integer projectId = 10;
        Project project = Project.builder().id(projectId).build();
        ZonedDateTime day1 = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime day10 = ZonedDateTime.of(2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC);

        // Intentionally returned out of order by repository to verify deterministic sorting (startDate ASC, id ASC)
        Sprint sprintSameStartLaterId = Sprint.builder()
                .id(3)
                .name("Sprint Zero Velocity")
                .project(project)
                .startDate(day1)
                .endDate(day1.plusDays(4))
                .active(false)
                .isDone(true)
                .build();
        Sprint sprintEarlierId = Sprint.builder()
                .id(1)
                .name("Sprint 1")
                .project(project)
                .startDate(day1)
                .endDate(day1.plusDays(5))
                .active(false)
                .isDone(true)
                .build();
        Sprint activeSprint = Sprint.builder()
                .id(5)
                .name("Active Sprint")
                .project(project)
                .startDate(day10)
                .endDate(null)
                .active(true)
                .isDone(false)
                .build();
        Sprint plannedDraftSprint = Sprint.builder()
                .id(2)
                .name("Planned Draft")
                .project(project)
                .startDate(day1)
                .active(false)
                .isDone(false)
                .build();

        Issue sprint1Done = Issue.builder()
                .id(11)
                .sprint(sprintEarlierId)
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(8)
                .build();
        Issue sprint3DoneNullEst = Issue.builder()
                .id(12)
                .sprint(sprintSameStartLaterId)
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(null)
                .build();
        Issue sprint5InProgress = Issue.builder()
                .id(13)
                .sprint(activeSprint)
                .issueStatus(new IssueStatus(IssueStatusID.IN_PROGRESS.getStatusID(), null))
                .estimation(5)
                .build();

        // Sprint 1 had 1 uncompleted issue moved back to backlog at stopSprint
        List<SprintIssueEvent> events = List.of(
                event(1, 11, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 8, day1),
                event(1, 99, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 3, day1),
                event(1, 11, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), 8, day1.plusDays(2)),
                event(1, null, SprintEventType.SPRINT_STOPPED, null, null, day1.plusDays(5))
        );

        when(sprintRepository.findAllByProjectIdOrderByStartDateAscIdAsc(projectId))
                .thenReturn(List.of(sprintSameStartLaterId, plannedDraftSprint, activeSprint, sprintEarlierId));
        when(issueRepository.findAllBySprintIdIn(List.of(1, 3, 5)))
                .thenReturn(List.of(sprint1Done, sprint3DoneNullEst, sprint5InProgress));
        when(sprintIssueEventRepository.findAllBySprintIdInOrderByOccurredAtAscIdAsc(List.of(1, 3, 5)))
                .thenReturn(events);

        List<SprintComparisonItemDTO> result = sprintAnalyticsService.getProjectSprintsComparison(projectId);

        verify(userService).verifyProjectAccess(projectId);
        assertEquals(3, result.size());

        // Order: id=1 (day1), id=3 (day1), id=5 (day10)
        SprintComparisonItemDTO first = result.get(0);
        assertEquals(projectId, first.getProjectId());
        assertEquals(1, first.getSprintId());
        assertEquals("Sprint 1", first.getSprintName());
        assertEquals(1, first.getCompletedIssueCount());
        assertEquals(1, first.getUncompletedIssueCount());
        assertEquals(8, first.getVelocity());

        SprintComparisonItemDTO second = result.get(1);
        assertEquals(3, second.getSprintId());
        assertEquals("Sprint Zero Velocity", second.getSprintName());
        assertEquals(1, second.getCompletedIssueCount());
        assertEquals(0, second.getUncompletedIssueCount());
        assertEquals(0, second.getVelocity());

        SprintComparisonItemDTO third = result.get(2);
        assertEquals(5, third.getSprintId());
        assertNull(third.getEndDate());
        assertEquals(0, third.getCompletedIssueCount());
        assertEquals(1, third.getUncompletedIssueCount());
        assertEquals(0, third.getVelocity());
    }

    @Test
    public void getProjectSprintsComparison_returnsEmptyListWhenProjectHasNoStartedOrDoneSprints() {
        Integer projectId = 20;
        Sprint plannedOnly = Sprint.builder()
                .id(1)
                .project(Project.builder().id(projectId).build())
                .active(false)
                .isDone(false)
                .build();

        when(sprintRepository.findAllByProjectIdOrderByStartDateAscIdAsc(projectId))
                .thenReturn(List.of(plannedOnly));

        List<SprintComparisonItemDTO> result = sprintAnalyticsService.getProjectSprintsComparison(projectId);

        assertTrue(result.isEmpty());
        verifyNoInteractions(issueRepository, sprintIssueEventRepository);
    }

    @Test
    public void getProjectSprintsComparison_throwsOnNullMissingProjectAndUnauthorizedAccess() {
        assertThrows(NullIdentifierException.class, () -> sprintAnalyticsService.getProjectSprintsComparison(null));

        doThrow(new ResourceNotFoundException(PROJECT_DOES_NOT_EXISTS))
                .when(userService).verifyProjectAccess(999);
        assertThrows(ResourceNotFoundException.class, () -> sprintAnalyticsService.getProjectSprintsComparison(999));

        doThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED))
                .when(userService).verifyProjectAccess(50);
        assertThrows(ProjectAccessDeniedException.class, () -> sprintAnalyticsService.getProjectSprintsComparison(50));

        verifyNoInteractions(sprintRepository, issueRepository, sprintIssueEventRepository);
    }
    private static final ZonedDateTime RANGE_BASE = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    // Sprint 1: 1 Nov 09:00 - 5 Nov 17:00 | Sprint 2: 6 Nov 09:00 - 10 Nov 17:00 | Sprint 3 (aktywny): od 11 Nov 09:00
    private List<Sprint> threeProjectSprints() {
        return List.of(
                rangeSprint(1, 10, RANGE_BASE, RANGE_BASE.plusDays(4).plusHours(8), false, true),
                rangeSprint(2, 10, RANGE_BASE.plusDays(5), RANGE_BASE.plusDays(9).plusHours(8), false, true),
                rangeSprint(3, 10, RANGE_BASE.plusDays(10), null, true, false)
        );
    }

    private Sprint rangeSprint(Integer id, Integer projectId, ZonedDateTime start, ZonedDateTime end, boolean active, boolean done) {
        return Sprint.builder()
                .id(id)
                .name("Sprint " + id)
                .project(Project.builder().id(projectId).build())
                .startDate(start)
                .endDate(end)
                .active(active)
                .isDone(done)
                .build();
    }

    private void stubProjectSprints(Integer projectId, List<Sprint> sprints) {
        when(sprintRepository.findAllByProjectIdOrderByStartDateAscIdAsc(projectId)).thenReturn(sprints);
        when(issueRepository.findAllBySprintIdIn(anyCollection())).thenReturn(Collections.emptyList());
        when(sprintIssueEventRepository.findAllBySprintIdInOrderByOccurredAtAscIdAsc(anyCollection()))
                .thenReturn(Collections.emptyList());
    }

    private List<Integer> sprintIds(List<SprintComparisonItemDTO> items) {
        return items.stream().map(SprintComparisonItemDTO::getSprintId).toList();
    }

    @Test
    public void getProjectSprintsComparison_withoutRangeReturnsAllQualifyingSprints() {
        stubProjectSprints(10, threeProjectSprints());

        assertEquals(List.of(1, 2, 3), sprintIds(sprintAnalyticsService.getProjectSprintsComparison(10, null, null)));
        assertEquals(List.of(1, 2, 3), sprintIds(sprintAnalyticsService.getProjectSprintsComparison(10)));
    }

    @Test
    public void getProjectSprintsComparison_returnsSprintsOverlappingRangeInStableOrder() {
        stubProjectSprints(10, threeProjectSprints());

        // 4 Nov - 7 Nov: nachodzi na sprint 1 (koniec 5 Nov) i sprint 2 (start 6 Nov), sprint 3 zaczyna się później
        List<SprintComparisonItemDTO> result = sprintAnalyticsService.getProjectSprintsComparison(
                10, RANGE_BASE.plusDays(3), RANGE_BASE.plusDays(6));

        assertEquals(List.of(1, 2), sprintIds(result));
        verify(userService).verifyProjectAccess(10);
    }

    @Test
    public void getProjectSprintsComparison_rangeBoundariesAreInclusive() {
        stubProjectSprints(10, threeProjectSprints());
        ZonedDateTime sprint1End = RANGE_BASE.plusDays(4).plusHours(8);
        ZonedDateTime sprint2Start = RANGE_BASE.plusDays(5);

        // from == koniec sprintu 1 -> sprint 1 nadal wchodzi
        assertEquals(List.of(1, 2, 3), sprintIds(sprintAnalyticsService.getProjectSprintsComparison(10, sprint1End, null)));
        // from tuż po końcu sprintu 1 -> sprint 1 odpada
        assertEquals(List.of(2, 3), sprintIds(sprintAnalyticsService.getProjectSprintsComparison(10, sprint1End.plusSeconds(1), null)));
        // to == start sprintu 2 -> sprint 2 nadal wchodzi
        assertEquals(List.of(1, 2), sprintIds(sprintAnalyticsService.getProjectSprintsComparison(10, null, sprint2Start)));
        // to tuż przed startem sprintu 2 -> sprint 2 odpada
        assertEquals(List.of(1), sprintIds(sprintAnalyticsService.getProjectSprintsComparison(10, null, sprint2Start.minusSeconds(1))));
    }

    @Test
    public void getProjectSprintsComparison_fromEqualToSingleInstantIsAllowed() {
        stubProjectSprints(10, threeProjectSprints());
        ZonedDateTime instantInsideSprint1 = RANGE_BASE.plusDays(2);

        List<SprintComparisonItemDTO> result = sprintAnalyticsService.getProjectSprintsComparison(
                10, instantInsideSprint1, instantInsideSprint1);

        assertEquals(List.of(1), sprintIds(result));
    }

    @Test
    public void getProjectSprintsComparison_activeSprintWithoutEndDateMatchesAnyLaterRange() {
        stubProjectSprints(10, threeProjectSprints());

        List<SprintComparisonItemDTO> result = sprintAnalyticsService.getProjectSprintsComparison(
                10, ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC), null);

        assertEquals(List.of(3), sprintIds(result));
        assertNull(result.get(0).getEndDate());
    }

    @Test
    public void getProjectSprintsComparison_returnsEmptyListAndSkipsDataLoadingWhenNothingOverlapsRange() {
        when(sprintRepository.findAllByProjectIdOrderByStartDateAscIdAsc(10)).thenReturn(threeProjectSprints());

        List<SprintComparisonItemDTO> result = sprintAnalyticsService.getProjectSprintsComparison(
                10, null, RANGE_BASE.minusDays(1));

        assertTrue(result.isEmpty());
        verifyNoInteractions(issueRepository, sprintIssueEventRepository);
    }

    @Test
    public void getProjectSprintsComparison_rangeFilterKeepsStrictProjectIsolation() {
        Sprint foreignSprint = rangeSprint(6, 99, RANGE_BASE, RANGE_BASE.plusDays(3), false, true);
        stubProjectSprints(10, List.of(threeProjectSprints().get(0), foreignSprint));

        List<SprintComparisonItemDTO> result = sprintAnalyticsService.getProjectSprintsComparison(
                10, RANGE_BASE, RANGE_BASE.plusDays(10));

        assertEquals(List.of(1), sprintIds(result));
        assertEquals(10, result.get(0).getProjectId());
    }

    @Test
    public void getProjectSprintsComparison_handlesLegacyAndUndatedSprintsWhenFiltering() {
        Sprint doneWithoutEndDate = rangeSprint(4, 10, RANGE_BASE, null, false, true);
        Sprint undated = rangeSprint(5, 10, null, null, false, true);
        stubProjectSprints(10, List.of(doneWithoutEndDate, undated));

        // bez filtra oba sprinty są zwracane
        assertEquals(List.of(4, 5), sprintIds(sprintAnalyticsService.getProjectSprintsComparison(10)));
        // z filtrem: zakończony sprint bez endDate = punkt w startDate (wychodzi poza from), sprint bez dat jest pomijany
        assertTrue(sprintAnalyticsService.getProjectSprintsComparison(10, RANGE_BASE.plusDays(1), null).isEmpty());
        assertEquals(List.of(4), sprintIds(sprintAnalyticsService.getProjectSprintsComparison(10, RANGE_BASE, RANGE_BASE)));
    }

    @Test
    public void getProjectSprintsComparison_throwsInvalidDateRangeWhenFromIsAfterTo() {
        InvalidDateRangeException exception = assertThrows(
                InvalidDateRangeException.class,
                () -> sprintAnalyticsService.getProjectSprintsComparison(10, RANGE_BASE.plusDays(1), RANGE_BASE)
        );

        assertEquals(INVALID_DATE_RANGE, exception.getMessage());
        verifyNoInteractions(sprintRepository, userService, issueRepository, sprintIssueEventRepository);
    }

    @Test
    public void exportProjectSprintsComparisonCsv_respectsRangeFilterAndFormatsCsv() {
        List<Sprint> sprints = List.of(
                rangeSprint(1, 10, RANGE_BASE, RANGE_BASE.plusDays(4), false, true),
                rangeSprint(2, 10, RANGE_BASE.plusDays(5), RANGE_BASE.plusDays(9), false, true)
        );
        Issue doneIssue = Issue.builder()
                .id(11)
                .sprint(sprints.get(0))
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(8)
                .build();

        when(sprintRepository.findAllByProjectIdOrderByStartDateAscIdAsc(10)).thenReturn(sprints);
        when(issueRepository.findAllBySprintIdIn(List.of(1))).thenReturn(List.of(doneIssue));
        when(sprintIssueEventRepository.findAllBySprintIdInOrderByOccurredAtAscIdAsc(List.of(1)))
                .thenReturn(Collections.emptyList());

        byte[] csvBytes = sprintAnalyticsService.exportProjectSprintsComparisonCsv(10, RANGE_BASE, RANGE_BASE.plusDays(2));
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        String expected = "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,uncompletedIssueCount,velocity\r\n"
                + "10,1,Sprint 1,2026-11-01T09:00:00Z,2026-11-05T09:00:00Z,1,0,8\r\n";
        assertEquals(expected, csv);
        verify(userService).verifyProjectAccess(10);
    }

    @Test
    public void exportProjectSprintsComparisonCsv_returnsHeaderOnlyWhenNoSprintsMatch() {
        when(sprintRepository.findAllByProjectIdOrderByStartDateAscIdAsc(10)).thenReturn(threeProjectSprints());

        byte[] csvBytes = sprintAnalyticsService.exportProjectSprintsComparisonCsv(10, null, RANGE_BASE.minusDays(1));
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertEquals(
                "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,uncompletedIssueCount,velocity\r\n",
                csv
        );
        verifyNoInteractions(issueRepository, sprintIssueEventRepository);
    }

    @Test
    public void exportProjectSprintsComparisonCsv_throwsInvalidDateRangeWhenFromIsAfterTo() {
        assertThrows(
                InvalidDateRangeException.class,
                () -> sprintAnalyticsService.exportProjectSprintsComparisonCsv(10, RANGE_BASE.plusDays(1), RANGE_BASE)
        );
        verifyNoInteractions(sprintRepository, userService, issueRepository, sprintIssueEventRepository);
    }

    @Test
    public void getBurndownChart_tracksChronologicalRemainingWorkWithScopeChanges() {
        Integer projectId = 10;
        Integer sprintId = 300;
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime endDate = ZonedDateTime.of(2026, 11, 5, 17, 0, 0, 0, ZoneOffset.UTC);

        Sprint sprint = Sprint.builder()
                .id(sprintId)
                .name("Burndown Sprint")
                .project(Project.builder().id(projectId).build())
                .startDate(startDate)
                .endDate(endDate)
                .isDone(true)
                .build();

        // Day 1 (Nov 1): issue 1 (5 pts, TO_DO), issue 2 (3 pts, TO_DO) -> remaining=8
        // Day 2 (Nov 2): issue 3 added (8 pts, TO_DO), issue 1 -> DONE (-5 pts) -> remaining=11
        // Day 3 (Nov 3): issue 2 removed (-3 pts) -> remaining=8
        // Day 4 (Nov 4): issue 3 estimation 8 -> 13 (+5 pts) -> remaining=13
        // Day 5 (Nov 5): issue 3 -> DONE (-13 pts), SPRINT_STOPPED -> remaining=0
        List<SprintIssueEvent> events = List.of(
                event(sprintId, 1, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 5, startDate),
                event(sprintId, 2, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), 3, startDate),
                event(sprintId, 3, SprintEventType.ADDED_TO_SPRINT, IssueStatusID.TO_DO.getStatusID(), 8, startDate.plusDays(1)),
                event(sprintId, 1, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), 5, startDate.plusDays(1).plusHours(2)),
                event(sprintId, 2, SprintEventType.REMOVED_FROM_SPRINT, IssueStatusID.TO_DO.getStatusID(), 3, startDate.plusDays(2)),
                event(sprintId, 3, SprintEventType.ESTIMATION_CHANGED, IssueStatusID.IN_PROGRESS.getStatusID(), 13, startDate.plusDays(3)),
                event(sprintId, 3, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), 13, startDate.plusDays(4)),
                event(sprintId, null, SprintEventType.SPRINT_STOPPED, null, null, endDate)
        );

        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId)).thenReturn(events);

        BurndownChartDTO chart = sprintAnalyticsService.getBurndownChart(sprintId);

        verify(userService).verifyProjectAccess(projectId);
        assertEquals(sprintId, chart.getSprintId());
        assertEquals("Burndown Sprint", chart.getSprintName());
        assertEquals(projectId, chart.getProjectId());
        assertEquals(startDate, chart.getStartDate());
        assertEquals(endDate, chart.getEndDate());
        assertEquals("STORY_POINTS", chart.getUnit());
        assertEquals(5, chart.getPoints().size());

        assertBurndownPoint(chart.getPoints().get(0), LocalDate.of(2026, 11, 1), 8);
        assertBurndownPoint(chart.getPoints().get(1), LocalDate.of(2026, 11, 2), 11);
        assertBurndownPoint(chart.getPoints().get(2), LocalDate.of(2026, 11, 3), 8);
        assertBurndownPoint(chart.getPoints().get(3), LocalDate.of(2026, 11, 4), 13);
        assertBurndownPoint(chart.getPoints().get(4), LocalDate.of(2026, 11, 5), 0);
    }

    @Test
    public void getBurndownChart_handlesNullOrNegativeEstimationsAndGuaranteesNonNegativeRemainingWork() {
        Integer sprintId = 301;
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime endDate = ZonedDateTime.of(2026, 11, 2, 17, 0, 0, 0, ZoneOffset.UTC);

        Sprint sprint = Sprint.builder()
                .id(sprintId)
                .name("Unestimated Sprint")
                .project(Project.builder().id(10).build())
                .startDate(startDate)
                .endDate(endDate)
                .isDone(true)
                .build();

        List<SprintIssueEvent> events = List.of(
                event(sprintId, 1, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), null, startDate),
                event(sprintId, 2, SprintEventType.SPRINT_STARTED, IssueStatusID.TO_DO.getStatusID(), -5, startDate),
                event(sprintId, 1, SprintEventType.STATUS_CHANGED, IssueStatusID.DONE.getStatusID(), null, startDate.plusDays(1)),
                event(sprintId, null, SprintEventType.SPRINT_STOPPED, null, null, endDate)
        );

        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId)).thenReturn(events);

        BurndownChartDTO chart = sprintAnalyticsService.getBurndownChart(sprintId);

        assertEquals(2, chart.getPoints().size());
        assertBurndownPoint(chart.getPoints().get(0), LocalDate.of(2026, 11, 1), 0);
        assertBurndownPoint(chart.getPoints().get(1), LocalDate.of(2026, 11, 2), 0);
    }

    @Test
    public void getBurndownChart_handlesLegacySprintWithoutEventsAndUnstartedSprint() {
        Integer legacySprintId = 302;
        Integer unstartedSprintId = 303;
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);

        Sprint legacySprint = Sprint.builder()
                .id(legacySprintId)
                .name("Legacy Burndown")
                .project(Project.builder().id(10).build())
                .startDate(startDate)
                .build();
        Sprint unstartedSprint = Sprint.builder()
                .id(unstartedSprintId)
                .name("Planned Burndown")
                .project(Project.builder().id(10).build())
                .startDate(null)
                .build();

        Issue doneIssue = Issue.builder()
                .id(1)
                .sprint(legacySprint)
                .issueStatus(new IssueStatus(IssueStatusID.DONE.getStatusID(), null))
                .estimation(5)
                .build();
        Issue todoIssue = Issue.builder()
                .id(2)
                .sprint(legacySprint)
                .issueStatus(new IssueStatus(IssueStatusID.TO_DO.getStatusID(), null))
                .estimation(3)
                .build();

        when(sprintRepository.findById(legacySprintId)).thenReturn(Optional.of(legacySprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(legacySprintId))
                .thenReturn(Collections.emptyList());
        when(issueRepository.findAllBySprintId(legacySprintId)).thenReturn(List.of(doneIssue, todoIssue));

        when(sprintRepository.findById(unstartedSprintId)).thenReturn(Optional.of(unstartedSprint));
        when(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(unstartedSprintId))
                .thenReturn(Collections.emptyList());

        BurndownChartDTO legacyChart = sprintAnalyticsService.getBurndownChart(legacySprintId);
        BurndownChartDTO unstartedChart = sprintAnalyticsService.getBurndownChart(unstartedSprintId);

        assertEquals(1, legacyChart.getPoints().size());
        assertBurndownPoint(legacyChart.getPoints().get(0), LocalDate.of(2026, 11, 1), 3);
        assertTrue(unstartedChart.getPoints().isEmpty());
    }

    @Test
    public void getBurndownChart_throwsOnNullMissingAndUnauthorizedSprint() {
        assertThrows(NullIdentifierException.class, () -> sprintAnalyticsService.getBurndownChart(null));

        when(sprintRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> sprintAnalyticsService.getBurndownChart(999));

        Sprint unauthorizedSprint = Sprint.builder().id(304).project(Project.builder().id(50).build()).build();
        when(sprintRepository.findById(304)).thenReturn(Optional.of(unauthorizedSprint));
        doThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED)).when(userService).verifyProjectAccess(50);

        assertThrows(ProjectAccessDeniedException.class, () -> sprintAnalyticsService.getBurndownChart(304));
    }

    private void assertBurndownPoint(BurndownPointDTO point, LocalDate expectedDate, int expectedRemaining) {
        assertEquals(expectedDate, point.getDate());
        assertEquals(expectedRemaining, point.getRemainingWork());
        assertEquals("STORY_POINTS", point.getUnit());
    }

    private void assertPoint(BurnupPointDTO point, LocalDate expectedDate, int expectedCompleted, int expectedScope) {
        assertEquals(expectedDate, point.getDate());
        assertEquals(expectedCompleted, point.getCompletedWork());
        assertEquals(expectedScope, point.getTotalScope());
        assertEquals("STORY_POINTS", point.getUnit());
    }

    private SprintIssueEvent event(
            Integer sprintId,
            Integer issueId,
            SprintEventType type,
            Integer statusId,
            Integer estimation,
            ZonedDateTime occurredAt
    ) {
        return SprintIssueEvent.builder()
                .sprintId(sprintId)
                .issueId(issueId)
                .eventType(type)
                .statusId(statusId)
                .estimation(estimation)
                .occurredAt(occurredAt)
                .build();
    }
}
