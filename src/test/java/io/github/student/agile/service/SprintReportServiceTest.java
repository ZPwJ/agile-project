package io.github.student.agile.service;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.dto.analytics.SprintReportDTO;
import io.github.student.agile.resource.dto.analytics.SprintState;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
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

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class SprintReportServiceTest {

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
    public void getSprintReport_finishedSprint_summarizesCompletedAndUncompletedIssues() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        Issue done = addIssue(sprint, 5);
        Issue doneWithoutEstimation = addIssue(sprint, null);
        addIssue(sprint, 3);
        addIssue(sprint, null);
        sprintService.startSprint(sprint.getId());
        complete(done);
        complete(doneWithoutEstimation);
        sprintService.stopSprint(project.getId());

        //when
        SprintReportDTO report = sprintReportService.getSprintReport(sprint.getId());

        //then
        assertEquals(sprint.getId(), report.getSprintID());
        assertEquals(SPRINT_NAME, report.getSprintName());
        assertEquals(project.getId(), report.getProjectID());
        assertEquals(SprintState.FINISHED, report.getState());
        assertNotNull(report.getStartedAt());
        assertNotNull(report.getEndedAt());
        assertTrue(report.isHistoryAvailable());
        assertEquals(4, report.getInitialIssueCount());
        assertEquals(2, report.getCompletedIssueCount());
        assertEquals(2, report.getUncompletedIssueCount());
        assertEquals(5, report.getVelocity());
        assertEquals(3, report.getUncompletedEstimation());
        assertEquals(SprintReportDTO.STORY_POINTS, report.getEstimationUnit());
    }

    @Test
    public void getSprintReport_scopeChangesDuringSprint_areReflectedInCounts() {

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
        SprintReportDTO report = sprintReportService.getSprintReport(sprint.getId());

        //then
        assertEquals(SprintState.ACTIVE, report.getState());
        assertEquals(2, report.getInitialIssueCount());
        assertEquals(1, report.getCompletedIssueCount());
        assertEquals(1, report.getUncompletedIssueCount());
        assertEquals(13, report.getVelocity());
        assertEquals(4, report.getUncompletedEstimation());
    }

    @Test
    public void getSprintReport_concernsOnlyGivenSprint() {

        //given
        Project otherProject = projectRepository.save(createProjectEntity());
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        Sprint otherSprint = sprintRepository.save(createSprintEntity(otherProject, FALSE));
        addIssue(sprint, 1);
        complete(addIssueAndStart(otherSprint, 20));
        sprintService.startSprint(sprint.getId());

        //when
        SprintReportDTO report = sprintReportService.getSprintReport(sprint.getId());

        //then
        assertEquals(1, report.getInitialIssueCount());
        assertEquals(0, report.getCompletedIssueCount());
        assertEquals(0, report.getVelocity());
        assertEquals(1, report.getUncompletedEstimation());
    }

    @Test
    public void getSprintReport_plannedSprint_usesCurrentIssues() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        addIssue(sprint, 5);
        addIssue(sprint, null);

        //when
        SprintReportDTO report = sprintReportService.getSprintReport(sprint.getId());

        //then
        assertEquals(SprintState.PLANNED, report.getState());
        assertFalse(report.isHistoryAvailable());
        assertEquals(2, report.getInitialIssueCount());
        assertEquals(0, report.getCompletedIssueCount());
        assertEquals(2, report.getUncompletedIssueCount());
        assertEquals(0, report.getVelocity());
        assertEquals(5, report.getUncompletedEstimation());
    }

    @Test
    public void getSprintReport_sprintFinishedWithoutHistory_returnsZeros() {

        //given: sprint finished before the analytics module recorded any events
        Sprint sprint = createSprintEntity(project, FALSE);
        sprint.setIsDone(TRUE);
        sprint.setSprintIssues(null);
        sprintRepository.save(sprint);

        //when
        SprintReportDTO report = sprintReportService.getSprintReport(sprint.getId());

        //then
        assertEquals(SprintState.FINISHED, report.getState());
        assertFalse(report.isHistoryAvailable());
        assertEquals(0, report.getInitialIssueCount());
        assertEquals(0, report.getCompletedIssueCount());
        assertEquals(0, report.getUncompletedIssueCount());
        assertEquals(0, report.getVelocity());
    }

    @Test
    public void getSprintReport_startedSprintWithoutIssuesLeft_returnsZeros() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(project, FALSE));
        Issue issue = addIssue(sprint, 5);
        sprintService.startSprint(sprint.getId());
        sprintService.deleteIssueFromSprint(sprint.getId(), issue.getId());

        //when
        SprintReportDTO report = sprintReportService.getSprintReport(sprint.getId());

        //then
        assertEquals(1, report.getInitialIssueCount());
        assertEquals(0, report.getCompletedIssueCount());
        assertEquals(0, report.getUncompletedIssueCount());
        assertEquals(0, report.getVelocity());
        assertEquals(0, report.getUncompletedEstimation());
    }

    @Test
    public void getSprintReport_throwsResourceNotFoundExceptionTest() {

        //given
        Integer fakeSprintID = -1;

        //when
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> sprintReportService.getSprintReport(fakeSprintID));

        //then
        assertEquals(SPRINT_DOES_NOT_EXISTS, exception.getMessage());
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
