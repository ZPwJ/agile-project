package io.github.student.agile.service;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO;
import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO.SprintResult;
import io.github.student.agile.resource.dto.analytics.SprintReportDTO;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.SprintComparisonService;
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

import java.time.ZonedDateTime;
import java.util.List;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntity;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class SprintComparisonServiceTest {

    @Autowired
    private SprintComparisonService sprintComparisonService;

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
    public void compareSprints_finishedSprints_returnsMetricsOfEachSprint() {

        //given
        Sprint first = plannedSprint(project, "Sprint 1");
        Issue done = addIssue(first, 5);
        Issue doneWithoutEstimation = addIssue(first, null);
        addIssue(first, 3);
        sprintService.startSprint(first.getId());
        complete(done);
        complete(doneWithoutEstimation);
        sprintService.stopSprint(project.getId());

        Sprint second = plannedSprint(project, "Sprint 2");
        Issue secondDone = addIssue(second, 8);
        addIssue(second, 2);
        sprintService.startSprint(second.getId());
        complete(secondDone);
        sprintService.stopSprint(project.getId());

        //when
        SprintComparisonDTO comparison = sprintComparisonService.compareSprints(project.getId());

        //then
        assertEquals(project.getId(), comparison.getProjectID());
        assertEquals(SprintReportDTO.STORY_POINTS, comparison.getEstimationUnit());
        assertEquals(2, comparison.getSprints().size());

        SprintResult firstResult = comparison.getSprints().get(0);
        assertEquals(first.getId(), firstResult.sprintID());
        assertEquals("Sprint 1", firstResult.sprintName());
        assertNotNull(firstResult.startedAt());
        assertNotNull(firstResult.endedAt());
        assertTrue(firstResult.historyAvailable());
        assertEquals(2, firstResult.completedIssueCount());
        assertEquals(1, firstResult.uncompletedIssueCount());
        assertEquals(5, firstResult.velocity());

        SprintResult secondResult = comparison.getSprints().get(1);
        assertEquals(second.getId(), secondResult.sprintID());
        assertEquals(1, secondResult.completedIssueCount());
        assertEquals(1, secondResult.uncompletedIssueCount());
        assertEquals(8, secondResult.velocity());
    }

    @Test
    public void compareSprints_metricsAreEqualToSprintReports() {

        //given: scope changes and an estimation change after completion
        Sprint sprint = plannedSprint(project, "Sprint 1");
        Issue removed = addIssue(sprint, 8);
        Issue completedThenReestimated = addIssue(sprint, 5);
        sprintService.startSprint(sprint.getId());
        Issue added = addIssue(sprint, 3);
        sprintService.deleteIssueFromSprint(sprint.getId(), removed.getId());
        complete(completedThenReestimated);
        changeEstimation(completedThenReestimated, 21);
        complete(added);
        sprintService.stopSprint(project.getId());

        //when
        SprintResult result = sprintComparisonService.compareSprints(project.getId()).getSprints().get(0);
        SprintReportDTO report = sprintReportService.getSprintReport(sprint.getId());

        //then
        assertEquals(8, result.velocity());
        assertEquals(report.getVelocity(), result.velocity());
        assertEquals(report.getCompletedIssueCount(), result.completedIssueCount());
        assertEquals(report.getUncompletedIssueCount(), result.uncompletedIssueCount());
        assertEquals(report.isHistoryAvailable(), result.historyAvailable());
        assertEquals(report.getStartedAt(), result.startedAt());
        assertEquals(report.getEndedAt(), result.endedAt());
    }

    @Test
    public void compareSprints_sprintWithoutCompletedIssues_isIncluded() {

        //given
        Sprint sprint = plannedSprint(project, "Sprint 1");
        addIssue(sprint, 5);
        addIssue(sprint, null);
        sprintService.startSprint(sprint.getId());
        sprintService.stopSprint(project.getId());

        //when
        List<SprintResult> sprints = sprintComparisonService.compareSprints(project.getId()).getSprints();

        //then
        assertEquals(1, sprints.size());
        assertEquals(sprint.getId(), sprints.get(0).sprintID());
        assertEquals(0, sprints.get(0).completedIssueCount());
        assertEquals(2, sprints.get(0).uncompletedIssueCount());
        assertEquals(0, sprints.get(0).velocity());
    }

    @Test
    public void compareSprints_ordersSprintsFromOldestToNewest() {

        //given: saved in a different order than they were run
        Sprint newest = finishedSprint(project, "Newest", ZonedDateTime.parse("2026-10-15T09:00:00+02:00"));
        Sprint oldest = finishedSprint(project, "Oldest", ZonedDateTime.parse("2026-09-01T09:00:00+02:00"));
        Sprint withoutHistory = finishedSprint(project, "Without history", null);
        Sprint sameStartAsNewest = finishedSprint(project, "Same start", ZonedDateTime.parse("2026-10-15T09:00:00+02:00"));

        //when
        List<SprintResult> sprints = sprintComparisonService.compareSprints(project.getId()).getSprints();

        //then
        assertEquals(
                List.of(withoutHistory.getId(), oldest.getId(), newest.getId(), sameStartAsNewest.getId()),
                sprints.stream().map(SprintResult::sprintID).toList());
    }

    @Test
    public void compareSprints_skipsSprintsThatAreNotFinished() {

        //given
        Sprint finished = plannedSprint(project, "Finished");
        addIssue(finished, 1);
        sprintService.startSprint(finished.getId());
        sprintService.stopSprint(project.getId());

        Sprint active = plannedSprint(project, "Active");
        addIssue(active, 2);
        sprintService.startSprint(active.getId());

        Sprint planned = plannedSprint(project, "Planned");
        addIssue(planned, 3);

        //when
        List<SprintResult> sprints = sprintComparisonService.compareSprints(project.getId()).getSprints();

        //then
        assertEquals(List.of(finished.getId()), sprints.stream().map(SprintResult::sprintID).toList());
    }

    @Test
    public void compareSprints_returnsOnlySprintsOfGivenProject() {

        //given
        Project otherProject = projectRepository.save(createProjectEntity());
        Sprint otherSprint = plannedSprint(otherProject, "Other project sprint");
        complete(addIssueAndStart(otherSprint, 20));
        sprintService.stopSprint(otherProject.getId());

        Sprint sprint = plannedSprint(project, "Sprint 1");
        complete(addIssueAndStart(sprint, 1));
        sprintService.stopSprint(project.getId());

        //when
        SprintComparisonDTO comparison = sprintComparisonService.compareSprints(project.getId());

        //then
        assertEquals(1, comparison.getSprints().size());
        assertEquals(sprint.getId(), comparison.getSprints().get(0).sprintID());
        assertEquals(1, comparison.getSprints().get(0).velocity());
    }

    @Test
    public void compareSprints_sprintFinishedWithoutHistory_isIncludedWithoutDates() {

        //given: sprint finished before the analytics module recorded any events
        Sprint sprint = finishedSprint(project, "Legacy", null);

        //when
        SprintResult result = sprintComparisonService.compareSprints(project.getId()).getSprints().get(0);

        //then
        assertEquals(sprint.getId(), result.sprintID());
        assertFalse(result.historyAvailable());
        assertNull(result.startedAt());
        assertNull(result.endedAt());
        assertEquals(0, result.completedIssueCount());
        assertEquals(0, result.uncompletedIssueCount());
        assertEquals(0, result.velocity());
    }

    @Test
    public void compareSprints_projectWithoutFinishedSprints_returnsEmptyList() {

        //given
        plannedSprint(project, "Planned");

        //when
        SprintComparisonDTO comparison = sprintComparisonService.compareSprints(project.getId());

        //then
        assertEquals(project.getId(), comparison.getProjectID());
        assertTrue(comparison.getSprints().isEmpty());
    }

    @Test
    public void compareSprints_notExistingProject_throwsResourceNotFoundException() {

        //when
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> sprintComparisonService.compareSprints(-1));

        //then
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    private Sprint plannedSprint(Project sprintProject, String name) {
        Sprint sprint = createSprintEntity(sprintProject, FALSE);
        sprint.setName(name);
        return sprintRepository.save(sprint);
    }

    private Sprint finishedSprint(Project sprintProject, String name, ZonedDateTime startedAt) {
        Sprint sprint = createSprintEntity(sprintProject, FALSE);
        sprint.setName(name);
        sprint.setIsDone(TRUE);
        sprint.setStartedAt(startedAt);
        sprint.setEndedAt(startedAt == null ? null : startedAt.plusDays(14));
        return sprintRepository.save(sprint);
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

    private Issue addIssueAndStart(Sprint sprint, Integer estimation) {
        Issue issue = addIssue(sprint, estimation);
        sprintService.startSprint(sprint.getId());
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
