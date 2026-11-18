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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTION_NOT_ALLOWED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
