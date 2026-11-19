package io.github.student.agile.service;

import io.github.student.agile.builder.SprintBuilder;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.domain.dictionary.SprintEventType;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.mapper.SprintDTOMapper;
import io.github.student.agile.resource.repository.BacklogRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.SprintService;
import io.github.student.agile.resource.service.UserService;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntity;
import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithIssueStatus;
import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithSprint;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.SPRINT_NAME;
import static io.github.student.agile.builder.SprintBuilder.createSprint;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTIVE_SPRINT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.EMPTY_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_ALREADY_HAS_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NO_ACTIVE_SPRINT_IN_PROJECT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_NAME_NOT_UNIQUE;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_START_DATE_IN_THE_FUTURE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class SprintServiceTest {

    @Autowired
    private SprintService sprintService;

    @Autowired
    private BacklogRepository backlogRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueStatusRepository issueStatusRepository;

    @Autowired
    private SprintDTOMapper sprintDTOMapper;

    @Autowired
    private SprintIssueEventRepository sprintIssueEventRepository;

    @MockBean
    private UserService userService;

    private Project defaultScrumProject;

    @BeforeEach
    void setUp() {

        Mockito.when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());

        Project project = createProjectEntity();
        projectRepository.save(project);
        defaultScrumProject = project;
    }

    @Test
    public void getActiveSprint_happyPathTest() {

        //given
        sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.TRUE));

        //when
        SprintDTO activeSprint = sprintService.getActiveSprint(defaultScrumProject.getId());

        //then
        assertEquals(Boolean.TRUE, activeSprint.getActive());
        assertEquals(SPRINT_NAME, activeSprint.getName());
        assertEquals(defaultScrumProject.getId(), activeSprint.getProjectID());
    }

    @Test
    public void getActiveSprint_returnsNullWhenThereIsNoActiveSprint() {

        //given + when
        SprintDTO activeSprint = sprintService.getActiveSprint(defaultScrumProject.getId());

        //then
        Assertions.assertNull(activeSprint);
    }

    @Test
    public void getActiveSprint_throwsProjectDoesNotExistsExceptionText() {

        //given
        Integer fakeProjectID = defaultScrumProject.getId() + 1;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.getActiveSprint(fakeProjectID));

        //then
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void getAllNonActivatedSprints_happyPathTest() {

        //given
        int sprintsAmount = 3;
        List<SprintDTO> sprintDTOList = new ArrayList<>(Collections.nCopies(3, SprintBuilder.createSprint(defaultScrumProject.getId())));
        long expectedSprintsAmount = sprintRepository.count() + sprintsAmount;

        //when
        List<SprintDTO> createdSprints = new ArrayList<>();
        sprintDTOList.forEach(sprint -> {
            sprint.setName(RandomStringUtils.random(15));
            createdSprints.add(sprintService.createSprint(sprint));
        });
        long actualSprintsAmount = sprintRepository.count();

        //then
        assertEquals(expectedSprintsAmount, actualSprintsAmount);
        createdSprints.forEach(created -> assertEquals(Boolean.FALSE, created.getActive()));
    }

    @Test
    public void createSprint_happyPathTest() {

        //given
        SprintDTO sprintDTO = createSprint(defaultScrumProject.getId());
        long expectedSprintsAmount = sprintRepository.count() + 1;

        //when
        SprintDTO createdSprint = sprintService.createSprint(sprintDTO);
        long actualSprintsAmount = sprintRepository.count();

        //then
        assertEquals(expectedSprintsAmount, actualSprintsAmount);
        assertEquals(sprintDTO.getName(), createdSprint.getName());
        assertEquals(sprintDTO.getStartTime(), createdSprint.getStartTime());
        assertEquals(sprintDTO.getProjectID(), createdSprint.getProjectID());
        assertEquals(Boolean.FALSE, createdSprint.getActive());
        assertEquals(sprintDTO.getSprintGoal(), createdSprint.getSprintGoal());
    }

    @Test
    public void createSprint_throwsSprintNameIsNotUniqueExceptionTest() {

        //given
        sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.createSprint(createSprint(defaultScrumProject.getId())));

        //then
        assertEquals(SPRINT_NAME_NOT_UNIQUE, exception.getMessage());
    }

    @Test
    public void createSprint_throwsProjectDoesNotExistsExceptionTest() {

        //given
        Integer fakeProjectID = defaultScrumProject.getId() + 1;
        SprintDTO sprintDTO = createSprint(fakeProjectID);

        //when
        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> sprintService.createSprint(sprintDTO));

        //then
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void addIssueToSprint_happyPathTest() {

        //given
        Integer sprintID = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE)).getId();
        Integer issueID = issueRepository.save(createIssueEntity()).getId();

        //when
        sprintService.addIssueToSprint(issueID, sprintID);
        Issue issueAfterSave = issueRepository.findById(issueID).isPresent() ? issueRepository.findById(issueID).get() : null;

        //then
        assertNotNull(issueAfterSave);
        assertEquals(sprintID, issueAfterSave.getSprint().getId());
    }

    @Test
    public void addIssueToSprint_throwsIssueDoesNotExistsExceptionTest() {

        //given
        Integer fakeIssueID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.addIssueToSprint(fakeIssueID, null));

        //then
        assertEquals(ISSUE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void addIssueToSprint_throwsSprintDoesNotExistsExceptionTest() {

        //given
        Integer issueID = issueRepository.save(createIssueEntity()).getId();
        Integer fakeSprintID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.addIssueToSprint(issueID, fakeSprintID));

        //then
        assertEquals(SPRINT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void addIssueToSprint_throwsIssueAlreadyHasSprintExceptionTest() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Issue issue = issueRepository.save(createIssueEntityWithSprint(sprint));

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.addIssueToSprint(issue.getId(), sprint.getId()));

        //then
        assertEquals(ISSUE_ALREADY_HAS_SPRINT, exception.getMessage());
    }

    @Test
    public void startSprint_happyPathTest() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Issue issue = issueRepository.save(createIssueEntityWithSprint(sprint));
        sprint.setSprintIssues(new HashSet<>(List.of(issue)));
        sprintRepository.save(sprint);

        //when
        sprintService.startSprint(sprint.getId());
        boolean existsActiveSprint = sprintRepository.findByActiveAndProject(Boolean.TRUE, defaultScrumProject).isPresent() ? sprintRepository.findByActiveAndProject(Boolean.TRUE, defaultScrumProject).get().getActive() : false;

        //then
        assertTrue(existsActiveSprint);
    }

    @Test
    public void startSprint_throwsActiveSprintAlreadyExistsExceptionTest() {

        //given
        sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.TRUE));
        Integer unactivatedSprintID = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE)).getId();

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.startSprint(unactivatedSprintID));

        //then
        assertEquals(ACTIVE_SPRINT_EXISTS, exception.getMessage());
    }

    @Test
    public void startSprint_throwsSprintDoesNotExistsExceptionTest() {

        //given
        Integer fakeSprintID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.startSprint(fakeSprintID));

        //then
        assertEquals(SPRINT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void startSprint_throwsEmptySprintExceptionTest() {

        //given
        Integer sprintWithoutIssuesID = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE)).getId();

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.startSprint(sprintWithoutIssuesID));

        //then
        assertEquals(EMPTY_SPRINT, exception.getMessage());
    }

    @Test
    public void startSprint_throwsSprintStartDateInTheFutureExceptionTest() {

        //given
        ZonedDateTime futureDate = ZonedDateTime.now().plusDays(1);

        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Issue issue = issueRepository.save(createIssueEntityWithSprint(sprint));
        sprint.setSprintIssues(new HashSet<>(List.of(issue)));
        sprint.setStartDate(futureDate);
        sprintRepository.save(sprint);

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.startSprint(sprint.getId()));

        //then
        assertEquals(SPRINT_START_DATE_IN_THE_FUTURE, exception.getMessage());
    }

    @Test
    public void stopSprint_happyPathTest() {

        //given
        SprintWithIssues preparedSprintToStop = prepareSprintWithIssuesToStop();
        Sprint activeSprint = preparedSprintToStop.sprint();
        Issue inProgressIssue = preparedSprintToStop.otherIssue();
        Issue doneIssue = preparedSprintToStop.doneIssue();

        //when
        sprintService.stopSprint(defaultScrumProject.getId());
        Sprint sprintAfterStopOperation = sprintRepository.findById(activeSprint.getId()).orElse(null);

        //then
        assertEquals(TO_DO.getStatusID(), inProgressIssue.getIssueStatus().getId());
        assertEquals(DONE.getStatusID(), doneIssue.getIssueStatus().getId());
        assertNull(inProgressIssue.getSprint());
        assertNotNull(sprintAfterStopOperation);
        assertEquals(activeSprint.getId(), doneIssue.getSprint().getId());
        assertEquals(Boolean.FALSE, sprintAfterStopOperation.getActive());
        assertEquals(Boolean.TRUE, sprintAfterStopOperation.getIsDone());
        assertNotNull(sprintAfterStopOperation.getEndDate());
        assertEquals(1, sprintAfterStopOperation.getSprintIssues().size());
        assertTrue(sprintAfterStopOperation.getSprintIssues().contains(doneIssue));
        assertFalse(sprintAfterStopOperation.getSprintIssues().contains(inProgressIssue));
    }

    @Test
    public void stopSprint_throwsProjectDoesNotExistsExceptionTest() {

        //given
        Integer fakeProjectID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.stopSprint(fakeProjectID));

        //then
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void stopSprint_throwsThereIsNoActiveSprintExceptionTest() {

        //given
        sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.stopSprint(defaultScrumProject.getId()));

        //then
        assertEquals(NO_ACTIVE_SPRINT_IN_PROJECT, exception.getMessage());
    }

    @Test
    public void deleteSprint_emptySprint_happyPathTest() {

        //given
        Integer sprintToDeleteID = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE)).getId();
        long expectedSprintsAmount = sprintRepository.count() - 1;

        //when
        sprintService.deleteSprint(sprintToDeleteID);
        long actualSprintsAmount = sprintRepository.count();

        //then
        assertEquals(expectedSprintsAmount, actualSprintsAmount);
    }

    @Test
    public void deleteSprint_withIssues_happyPathTest() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Issue issue = issueRepository.save(createIssueEntityWithSprint(sprint));
        sprint.setSprintIssues(new HashSet<>(Sets.newSet(issue)));
        sprintRepository.save(sprint);
        long expectedSprintsAmount = sprintRepository.count() - 1;

        //when
        sprintService.deleteSprint(sprint.getId());
        long actualSprintsAmount = sprintRepository.count();

        //then
        assertEquals(expectedSprintsAmount, actualSprintsAmount);
        assertNull(issue.getSprint());
    }

    @Test
    public void deleteSprint_throwsSprintDoesNotExistsExceptionTest() {

        //given
        Integer fakeSprintID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.deleteSprint(fakeSprintID));

        //then
        assertEquals(SPRINT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void deleteIssueFromSprint_happyPathTest() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Issue issue = issueRepository.save(createIssueEntityWithSprint(sprint));
        sprint.setSprintIssues(new HashSet<>(Sets.newSet(issue)));
        sprintRepository.save(sprint);

        //when
        sprintService.deleteIssueFromSprint(sprint.getId(), issue.getId());

        //then
        assertNull(issue.getSprint());
        assertFalse(sprint.getSprintIssues().contains(issue));
    }

    @Test
    public void deleteIssueFromSprint_throwsSprintDoesNotExistsExceptionTest() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        issueRepository.save(createIssueEntityWithSprint(sprint));
        Integer fakeSprintID = sprint.getId() + 1;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.deleteIssueFromSprint(fakeSprintID, null));

        //then
        assertEquals(SPRINT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void deleteIssueFromSprint_throwsIssueDoesNotExistsExceptionTest() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Issue issue = issueRepository.save(createIssueEntityWithSprint(sprint));
        Integer fakeIssueID = issue.getId() + 1;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.deleteIssueFromSprint(sprint.getId(), fakeIssueID));

        //then
        assertEquals(ISSUE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void stopSprint_completedSprintIsExcludedFromActiveAndNonActivatedLists() {

        //given
        prepareSprintWithIssuesToStop();

        //when
        sprintService.stopSprint(defaultScrumProject.getId());

        //then
        assertNull(sprintService.getActiveSprint(defaultScrumProject.getId()));
        assertTrue(sprintService.getAllNonActivatedSprints(defaultScrumProject.getId()).isEmpty());
    }

    @Test
    public void sprintRepository_findsAllAndCompletedSprintsForProjectInOrder() {

        //given
        Project otherProject = createProjectEntity();
        otherProject.setName("Other project");
        projectRepository.save(otherProject);

        ZonedDateTime baseTime = ZonedDateTime.now().minusDays(10);

        Sprint completedEarlier = createSprintEntity(defaultScrumProject, Boolean.FALSE);
        completedEarlier.setName("Sprint 1");
        completedEarlier.setStartDate(baseTime);
        completedEarlier.setEndDate(baseTime.plusDays(3));
        completedEarlier.setIsDone(Boolean.TRUE);
        sprintRepository.save(completedEarlier);

        Sprint completedLater = createSprintEntity(defaultScrumProject, Boolean.FALSE);
        completedLater.setName("Sprint 2");
        completedLater.setStartDate(baseTime.plusDays(4));
        completedLater.setEndDate(baseTime.plusDays(7));
        completedLater.setIsDone(Boolean.TRUE);
        sprintRepository.save(completedLater);

        Sprint plannedSprint = createSprintEntity(defaultScrumProject, Boolean.FALSE);
        plannedSprint.setName("Sprint 3");
        plannedSprint.setStartDate(baseTime.plusDays(8));
        plannedSprint.setIsDone(Boolean.FALSE);
        sprintRepository.save(plannedSprint);

        Sprint otherProjectCompletedSprint = createSprintEntity(otherProject, Boolean.FALSE);
        otherProjectCompletedSprint.setName("Other Project Sprint");
        otherProjectCompletedSprint.setStartDate(baseTime);
        otherProjectCompletedSprint.setIsDone(Boolean.TRUE);
        sprintRepository.save(otherProjectCompletedSprint);

        //when
        List<Sprint> allProjectSprints = sprintRepository.findAllByProjectIdOrderByStartDateAscIdAsc(defaultScrumProject.getId());
        List<Sprint> completedProjectSprints = sprintRepository.findAllByProjectIdAndIsDoneTrueOrderByStartDateAscIdAsc(defaultScrumProject.getId());

        //then
        assertEquals(List.of(completedEarlier.getId(), completedLater.getId(), plannedSprint.getId()),
                allProjectSprints.stream().map(Sprint::getId).toList());
        assertEquals(List.of(completedEarlier.getId(), completedLater.getId()),
                completedProjectSprints.stream().map(Sprint::getId).toList());
    }

    @Test
    public void sprintDTOMapper_mapsEndDate() {

        //given
        ZonedDateTime endDate = ZonedDateTime.now();
        Sprint sprint = createSprintEntity(defaultScrumProject, Boolean.FALSE);
        sprint.setIsDone(Boolean.TRUE);
        sprint.setEndDate(endDate);

        //when
        SprintDTO dto = sprintDTOMapper.map(sprint);

        //then
        assertEquals(endDate, dto.getEndDate());
        assertEquals(sprint.getStartDate(), dto.getStartTime());
        assertEquals(defaultScrumProject.getId(), dto.getProjectID());
    }

    @Test
    public void issueStatusID_isCompletedRecognizesOnlyDoneStatus() {

        //given
        IssueStatus doneStatus = issueStatusRepository.getReferenceById(DONE.getStatusID());
        IssueStatus inProgressStatus = issueStatusRepository.getReferenceById(IN_PROGRESS.getStatusID());
        Issue doneIssue = createIssueEntityWithIssueStatus(doneStatus);
        Issue inProgressIssue = createIssueEntityWithIssueStatus(inProgressStatus);
        Issue issueWithoutStatus = createIssueEntity();

        //then
        assertTrue(IssueStatusID.isCompleted(doneStatus));
        assertTrue(IssueStatusID.isCompleted(doneIssue));
        assertFalse(IssueStatusID.isCompleted(inProgressStatus));
        assertFalse(IssueStatusID.isCompleted(inProgressIssue));
        assertFalse(IssueStatusID.isCompleted((IssueStatus) null));
        assertFalse(IssueStatusID.isCompleted((Issue) null));
        assertFalse(IssueStatusID.isCompleted(issueWithoutStatus));
    }

    @Test
    public void startSprint_recordsInitialIssuesAndEstimationsAsSprintStartedEvents() {

        //given
        IssueStatus toDoStatus = issueStatusRepository.getReferenceById(TO_DO.getStatusID());
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));

        Issue estimatedIssue = createIssueEntityWithIssueStatus(toDoStatus);
        estimatedIssue.setEstimation(5);
        estimatedIssue.setSprint(sprint);
        issueRepository.save(estimatedIssue);

        Issue unestimatedIssue = createIssueEntityWithIssueStatus(toDoStatus);
        unestimatedIssue.setEstimation(null);
        unestimatedIssue.setSprint(sprint);
        issueRepository.save(unestimatedIssue);

        sprint.setSprintIssues(new HashSet<>(Set.of(estimatedIssue, unestimatedIssue)));
        sprintRepository.save(sprint);

        //when
        sprintService.startSprint(sprint.getId());
        List<SprintIssueEvent> events = sprintIssueEventRepository
                .findAllBySprintIdAndEventTypeOrderByOccurredAtAscIdAsc(sprint.getId(), SprintEventType.SPRINT_STARTED);

        //then
        assertEquals(2, events.size());
        SprintIssueEvent firstEvent = events.stream()
                .filter(e -> estimatedIssue.getId().equals(e.getIssueId()))
                .findFirst()
                .orElseThrow();
        SprintIssueEvent secondEvent = events.stream()
                .filter(e -> unestimatedIssue.getId().equals(e.getIssueId()))
                .findFirst()
                .orElseThrow();
        assertEquals(5, firstEvent.getEstimation());
        assertEquals(TO_DO.getStatusID(), firstEvent.getStatusId());
        assertNotNull(firstEvent.getOccurredAt());
        assertNull(secondEvent.getEstimation());
        assertEquals(TO_DO.getStatusID(), secondEvent.getStatusId());
    }

    @Test
    public void sprintScopeChanges_recordEventsOnlyWhileSprintIsActiveAndPreserveChronology() {

        //given
        IssueStatus toDoStatus = issueStatusRepository.getReferenceById(TO_DO.getStatusID());
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));

        Issue initialIssue = createIssueEntityWithIssueStatus(toDoStatus);
        initialIssue.setEstimation(3);
        issueRepository.save(initialIssue);

        Issue addedDuringSprint = createIssueEntityWithIssueStatus(toDoStatus);
        addedDuringSprint.setEstimation(8);
        issueRepository.save(addedDuringSprint);

        //when: planning before start does not create scope events
        sprintService.addIssueToSprint(initialIssue.getId(), sprint.getId());
        assertTrue(sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprint.getId()).isEmpty());

        //when: start, add, remove, stop
        sprintService.startSprint(sprint.getId());
        sprintService.addIssueToSprint(addedDuringSprint.getId(), sprint.getId());
        sprintService.deleteIssueFromSprint(sprint.getId(), initialIssue.getId());
        sprintService.stopSprint(defaultScrumProject.getId());

        List<SprintIssueEvent> timeline = sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprint.getId());
        Sprint stoppedSprint = sprintRepository.findById(sprint.getId()).orElseThrow();

        //then
        assertEquals(
                List.of(
                        SprintEventType.SPRINT_STARTED,
                        SprintEventType.ADDED_TO_SPRINT,
                        SprintEventType.REMOVED_FROM_SPRINT,
                        SprintEventType.SPRINT_STOPPED
                ),
                timeline.stream().map(SprintIssueEvent::getEventType).toList()
        );
        assertEquals(initialIssue.getId(), timeline.get(0).getIssueId());
        assertEquals(addedDuringSprint.getId(), timeline.get(1).getIssueId());
        assertEquals(8, timeline.get(1).getEstimation());
        assertEquals(initialIssue.getId(), timeline.get(2).getIssueId());
        assertEquals(3, timeline.get(2).getEstimation());
        assertNull(timeline.get(3).getIssueId());
        assertEquals(stoppedSprint.getEndDate(), timeline.get(3).getOccurredAt());
    }

    private SprintWithIssues prepareSprintWithIssuesToStop() {

        // get issue proper statuses
        IssueStatus inProgressStatus = issueStatusRepository.getReferenceById(IN_PROGRESS.getStatusID());
        IssueStatus doneStatus = issueStatusRepository.getReferenceById(DONE.getStatusID());

        // create issues with statuses
        Issue inProgressIssue = createIssueEntityWithIssueStatus(inProgressStatus);
        Issue doneIssue = createIssueEntityWithIssueStatus(doneStatus);

        //create sprint
        Sprint activeSprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.TRUE));

        // assign sprint to issues
        inProgressIssue.setSprint(activeSprint);
        doneIssue.setSprint(activeSprint);
        issueRepository.save(inProgressIssue);
        issueRepository.save(doneIssue);

        // locate issues in backlog and sprint
        Backlog backlog = defaultScrumProject.getBacklog();
        backlog.setIssues(new HashSet<>(Set.of(inProgressIssue, doneIssue)));
        backlogRepository.save(backlog);
        activeSprint.setSprintIssues(new HashSet<>(Set.of(inProgressIssue, doneIssue)));
        sprintRepository.save(activeSprint);

        // this returned object is only for this method purpose
        return new SprintWithIssues(doneIssue, inProgressIssue, activeSprint);
    }

    private record SprintWithIssues(Issue doneIssue, Issue otherIssue, Sprint sprint){}
}
