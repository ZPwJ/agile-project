package io.github.student.agile.service;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.dto.analytics.SprintBurndownDTO;
import io.github.student.agile.resource.dto.analytics.SprintBurnupDTO;
import io.github.student.agile.resource.dto.analytics.SprintReportDTO;
import io.github.student.agile.resource.dto.analytics.SprintState;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.SprintChartService;
import io.github.student.agile.resource.service.SprintReportService;
import io.github.student.agile.resource.service.SprintService;
import io.github.student.agile.resource.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntity;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.SPRINT_NAME;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.CODE_REVIEW;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_TESTS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the whole sprint lifecycle; all events happen on the same day, so the charts have the start point (day 0)
 * and the point for the current day.
 */
@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class SprintChartServiceTest {

    @Autowired
    private SprintChartService sprintChartService;

    @Autowired
    private SprintReportService sprintReportService;

    @Autowired
    private SprintService sprintService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueStatusRepository issueStatusRepository;

    @MockBean
    private UserService userService;

    private Project project;

    @BeforeEach
    void setUp() {

        Mockito.when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        project = projectRepository.save(createProjectEntity());
    }

    @Test
    public void getBurndown_finishedSprint_startsAtInitialScopeAndBurnsCompletedWork() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        Issue done = addIssue(sprint, 5);
        addIssue(sprint, 3);
        addIssue(sprint, null);
        sprintService.startSprint(sprint.getId());
        complete(done);
        sprintService.stopSprint(project.getId());

        //when
        SprintBurndownDTO burndown = sprintChartService.getBurndown(sprint.getId());

        //then
        assertEquals(sprint.getId(), burndown.getSprintID());
        assertEquals(SPRINT_NAME, burndown.getSprintName());
        assertEquals(project.getId(), burndown.getProjectID());
        assertEquals(SprintState.FINISHED, burndown.getState());
        assertNotNull(burndown.getStartedAt());
        assertNotNull(burndown.getEndedAt());
        assertTrue(burndown.isHistoryAvailable());
        assertEquals(8, burndown.getInitialScope());
        assertEquals(SprintReportDTO.STORY_POINTS, burndown.getEstimationUnit());
        assertEquals("Europe/Warsaw", burndown.getTimeZone());
        assertEquals(new SprintBurndownDTO.Point(0, burndown.getPoints().get(0).date(), 8), burndown.getPoints().get(0));
        assertEquals(3, lastOf(burndown.getPoints()).remainingWork());
        assertChronological(burndown.getPoints().stream().map(SprintBurndownDTO.Point::day).toList());
    }

    @Test
    public void getBurndown_completedWorkAboveInitialScope_neverGoesBelowZero() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        Issue initial = addIssue(sprint, 2);
        sprintService.startSprint(sprint.getId());
        Issue added = addIssue(sprint, 13);
        complete(initial);
        complete(added);

        //when
        SprintBurndownDTO burndown = sprintChartService.getBurndown(sprint.getId());

        //then
        assertEquals(SprintState.ACTIVE, burndown.getState());
        assertEquals(2, burndown.getInitialScope());
        assertEquals(0, lastOf(burndown.getPoints()).remainingWork());
        assertTrue(burndown.getPoints().stream().allMatch(point -> point.remainingWork() >= 0));
    }

    @Test
    public void getBurnup_scopeChangesDuringSprint_areReflectedInTotalScope() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        Issue removed = addIssue(sprint, 8);
        Issue kept = addIssue(sprint, 2);
        sprintService.startSprint(sprint.getId());
        Issue added = addIssue(sprint, 13);
        sprintService.deleteIssueFromSprint(sprint.getId(), removed.getId());
        changeEstimation(kept, 4);
        complete(added);

        //when
        SprintBurnupDTO burnup = sprintChartService.getBurnup(sprint.getId());

        //then
        assertTrue(burnup.isHistoryAvailable());
        assertEquals(SprintReportDTO.STORY_POINTS, burnup.getEstimationUnit());
        assertEquals("Europe/Warsaw", burnup.getTimeZone());
        assertEquals(new SprintBurnupDTO.Point(0, burnup.getPoints().get(0).date(), 0, 10), burnup.getPoints().get(0));
        SprintBurnupDTO.Point today = lastOf(burnup.getPoints());
        assertEquals(13, today.completedWork());
        assertEquals(17, today.totalScope());
        assertChronological(burnup.getPoints().stream().map(SprintBurnupDTO.Point::day).toList());
    }

    @Test
    public void getBurnup_lastPoint_matchesSprintReport() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        Issue first = addIssue(sprint, 5);
        Issue second = addIssue(sprint, 3);
        addIssue(sprint, 8);
        sprintService.startSprint(sprint.getId());
        complete(first);
        complete(second);
        changeEstimation(second, 21);
        sprintService.stopSprint(project.getId());

        //when
        SprintBurnupDTO burnup = sprintChartService.getBurnup(sprint.getId());
        SprintReportDTO report = sprintReportService.getSprintReport(sprint.getId());

        //then
        SprintBurnupDTO.Point lastPoint = lastOf(burnup.getPoints());
        assertEquals(report.getVelocity(), lastPoint.completedWork());
        assertEquals(report.getVelocity() + report.getUncompletedEstimation(), lastPoint.totalScope());
    }

    @Test
    public void getBurnup_completedWork_excludesOtherSprints() {

        //given
        Project otherProject = projectRepository.save(createProjectEntity());
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        Sprint otherSprint = sprintRepository.save(createSprintEntity(otherProject, FALSE));
        addIssue(sprint, 1);
        Issue otherIssue = addIssue(otherSprint, 20);
        sprintService.startSprint(sprint.getId());
        sprintService.startSprint(otherSprint.getId());
        complete(otherIssue);

        //when
        SprintBurnupDTO burnup = sprintChartService.getBurnup(sprint.getId());

        //then
        assertTrue(burnup.getPoints().stream().allMatch(point -> point.completedWork() == 0 && point.totalScope() == 1));
    }

    @Test
    public void getCharts_plannedSprint_returnEmptyPoints() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        addIssue(sprint, 5);

        //when
        SprintBurndownDTO burndown = sprintChartService.getBurndown(sprint.getId());
        SprintBurnupDTO burnup = sprintChartService.getBurnup(sprint.getId());

        //then
        assertEquals(SprintState.PLANNED, burndown.getState());
        assertFalse(burndown.isHistoryAvailable());
        assertEquals(0, burndown.getInitialScope());
        assertTrue(burndown.getPoints().isEmpty());
        assertFalse(burnup.isHistoryAvailable());
        assertTrue(burnup.getPoints().isEmpty());
    }

    @Test
    public void getCharts_sprintFinishedWithoutHistory_returnEmptyPoints() {

        //given: sprint finished before the analytics module recorded any events
        Sprint sprint = createSprintEntity(project, FALSE);
        sprint.setIsDone(TRUE);
        sprintRepository.save(sprint);

        //when
        SprintBurndownDTO burndown = sprintChartService.getBurndown(sprint.getId());
        SprintBurnupDTO burnup = sprintChartService.getBurnup(sprint.getId());

        //then
        assertEquals(SprintState.FINISHED, burndown.getState());
        assertTrue(burndown.getPoints().isEmpty());
        assertTrue(burnup.getPoints().isEmpty());
    }

    @Test
    public void getCharts_sprintWithoutEstimatedIssues_returnZeroPoints() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        Issue issue = addIssue(sprint, null);
        sprintService.startSprint(sprint.getId());
        complete(issue);

        //when
        SprintBurndownDTO burndown = sprintChartService.getBurndown(sprint.getId());
        SprintBurnupDTO burnup = sprintChartService.getBurnup(sprint.getId());

        //then
        assertTrue(burndown.isHistoryAvailable());
        assertFalse(burndown.getPoints().isEmpty());
        assertTrue(burndown.getPoints().stream().allMatch(point -> point.remainingWork() == 0));
        assertTrue(burnup.getPoints().stream().allMatch(point -> point.completedWork() == 0 && point.totalScope() == 0));
    }

    @Test
    public void getBurndown_throwsResourceNotFoundExceptionTest() {

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> sprintChartService.getBurndown(-1));

        assertEquals(SPRINT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void getBurnup_throwsResourceNotFoundExceptionTest() {

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> sprintChartService.getBurnup(-1));

        assertEquals(SPRINT_DOES_NOT_EXISTS, exception.getMessage());
    }

    private static <T> T lastOf(List<T> points) {
        return points.get(points.size() - 1);
    }

    private static void assertChronological(List<Integer> days) {
        for (int i = 0; i < days.size(); i++) {
            assertEquals(i, days.get(i));
        }
    }

    private Issue addIssue(Sprint sprint, Integer estimation) {
        Issue issue = createIssueEntity();
        issue.setEstimation(estimation);
        issue.setProjectID(sprint.getProject().getId());
        issue.setIssueStatus(issueStatusRepository.findById(TO_DO.getStatusID()).orElseThrow());
        issueRepository.save(issue);
        sprintService.addIssueToSprint(issue.getId(), sprint.getId());
        return issue;
    }

    private void complete(Issue issue) {
        for (IssueStatusID status : new IssueStatusID[]{IN_PROGRESS, CODE_REVIEW, IN_TESTS, DONE}) {
            issueService.changeIssueStatus(issue.getId(), status.getStatusID());
        }
    }

    private void changeEstimation(Issue issue, Integer estimation) {
        issueService.updateIssue(IssueMRO.builder()
                .issueID(issue.getId())
                .name(issue.getName())
                .description(issue.getDescription())
                .estimation(estimation)
                .build());
    }
}
