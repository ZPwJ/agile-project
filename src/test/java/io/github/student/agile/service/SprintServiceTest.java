package io.github.student.agile.service;

import io.github.student.agile.builder.SprintBuilder;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.repository.BacklogRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.SprintService;
import io.github.student.agile.resource.service.UserService;
import jakarta.persistence.EntityManager;
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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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
import static io.github.student.agile.exception.dictionary.ExceptionMessages.DONE_ISSUE_CANNOT_BE_ADDED_TO_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.EMPTY_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.FINISHED_SPRINT_CANNOT_BE_EXTENDED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.FINISHED_SPRINT_CANNOT_BE_STARTED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_ALREADY_HAS_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_FROM_ANOTHER_PROJECT;
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
    private EntityManager entityManager;

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
        Sprint savedSprint = sprintRepository.findById(createdSprint.getId()).orElseThrow();
        assertNull(savedSprint.getStartedAt());
        assertNull(savedSprint.getEndedAt());
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
        Integer issueID = issueRepository.save(createIssueOfProject(defaultScrumProject)).getId();

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
    public void addIssueToSprint_throwsIssueFromAnotherProjectExceptionTest() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Project anotherProject = projectRepository.save(createProjectEntity());
        Issue issue = issueRepository.save(createIssueOfProject(anotherProject));

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.addIssueToSprint(issue.getId(), sprint.getId()));

        //then
        assertEquals(ISSUE_FROM_ANOTHER_PROJECT, exception.getMessage());
        assertNull(issue.getSprint());
    }

    @Test
    public void addIssueToSprint_throwsWhenIssueHasNoProject() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Issue issue = issueRepository.save(createIssueEntity());

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.addIssueToSprint(issue.getId(), sprint.getId()));

        //then
        assertEquals(ISSUE_FROM_ANOTHER_PROJECT, exception.getMessage());
        assertNull(issue.getSprint());
    }

    @Test
    public void addIssueToSprint_throwsDoneIssueCannotBeAddedExceptionTest() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Issue doneIssue = createIssueOfProject(defaultScrumProject);
        doneIssue.setIssueStatus(issueStatusRepository.findById(DONE.getStatusID()).orElseThrow());
        issueRepository.save(doneIssue);

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.addIssueToSprint(doneIssue.getId(), sprint.getId()));

        //then
        assertEquals(DONE_ISSUE_CANNOT_BE_ADDED_TO_SPRINT, exception.getMessage());
        assertNull(doneIssue.getSprint());
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
    public void startSprint_setsStartedAtAndKeepsPlannedStartDate() {

        //given
        ZonedDateTime plannedStartDate = ZonedDateTime.now().minusDays(3);
        Sprint sprint = saveSprintWithIssue();
        sprint.setStartDate(plannedStartDate);
        sprintRepository.save(sprint);
        Instant before = now();

        //when
        sprintService.startSprint(sprint.getId());

        //then
        assertBetween(before, sprint.getStartedAt(), now());
        assertEquals(plannedStartDate, sprint.getStartDate());
        assertNull(sprint.getEndedAt());
    }

    @Test
    public void startSprint_rejectedStartLeavesStartedAtEmpty() {

        //given
        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));

        //when
        assertThrows(RuntimeException.class, () -> sprintService.startSprint(sprint.getId()));

        //then
        assertNull(sprint.getStartedAt());
        assertFalse(sprint.getActive());
    }

    @Test
    public void startSprint_throwsFinishedSprintCannotBeStartedExceptionTest() {

        //given
        Sprint sprint = saveSprintWithIssue();
        Instant startedAt = now().minus(14, ChronoUnit.DAYS);
        Instant endedAt = now().minus(1, ChronoUnit.DAYS);
        sprint.setIsDone(Boolean.TRUE);
        sprint.setStartedAt(startedAt);
        sprint.setEndedAt(endedAt);
        sprintRepository.save(sprint);

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.startSprint(sprint.getId()));

        //then
        assertEquals(FINISHED_SPRINT_CANNOT_BE_STARTED, exception.getMessage());
        assertFalse(sprint.getActive());
        assertEquals(startedAt, sprint.getStartedAt());
        assertEquals(endedAt, sprint.getEndedAt());
    }

    @Test
    public void addIssueToSprint_throwsFinishedSprintCannotBeExtendedExceptionTest() {

        //given
        Sprint sprint = createSprintEntity(defaultScrumProject, Boolean.FALSE);
        sprint.setIsDone(Boolean.TRUE);
        sprintRepository.save(sprint);
        Issue issue = issueRepository.save(createIssueEntity());

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> sprintService.addIssueToSprint(issue.getId(), sprint.getId()));

        //then
        assertEquals(FINISHED_SPRINT_CANNOT_BE_EXTENDED, exception.getMessage());
        assertNull(issue.getSprint());
        assertTrue(sprint.getSprintIssues().isEmpty());
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
        Instant startedAt = now().minus(14, ChronoUnit.DAYS);
        activeSprint.setStartedAt(startedAt);
        sprintRepository.save(activeSprint);
        Instant before = now();

        //when
        sprintService.stopSprint(defaultScrumProject.getId());
        Instant after = now();
        entityManager.flush();
        entityManager.clear();

        //then
        Sprint sprintAfterStopOperation = sprintRepository.findById(activeSprint.getId()).orElseThrow();
        assertTrue(sprintAfterStopOperation.getIsDone());
        assertFalse(sprintAfterStopOperation.getActive());
        assertEquals(startedAt, sprintAfterStopOperation.getStartedAt());
        assertBetween(before, sprintAfterStopOperation.getEndedAt(), after);
        assertTrue(issueRepository.findAllBySprint(sprintAfterStopOperation).isEmpty());

        Issue inProgressIssue = issueRepository.findById(preparedSprintToStop.otherIssue().getId()).orElseThrow();
        Issue doneIssue = issueRepository.findById(preparedSprintToStop.doneIssue().getId()).orElseThrow();
        assertEquals(TO_DO.getStatusID(), inProgressIssue.getIssueStatus().getId());
        assertEquals(DONE.getStatusID(), doneIssue.getIssueStatus().getId());
        assertNull(inProgressIssue.getSprint());
        assertNull(doneIssue.getSprint());
    }

    @Test
    public void stopSprint_finishedSprintIsNotListedAsNonActivated() {

        //given
        Sprint activeSprint = prepareSprintWithIssuesToStop().sprint();
        Sprint plannedSprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));

        //when
        sprintService.stopSprint(defaultScrumProject.getId());
        List<SprintDTO> nonActivatedSprints = sprintService.getAllNonActivatedSprints(defaultScrumProject.getId());

        //then
        assertTrue(sprintRepository.existsById(activeSprint.getId()));
        assertEquals(List.of(plannedSprint.getId()), nonActivatedSprints.stream().map(SprintDTO::getId).toList());
    }

    @Test
    public void stopSprint_allowsToStartNextSprint() {

        //given
        prepareSprintWithIssuesToStop();
        Sprint nextSprint = saveSprintWithIssue();

        //when
        sprintService.stopSprint(defaultScrumProject.getId());
        sprintService.startSprint(nextSprint.getId());

        //then
        assertTrue(nextSprint.getActive());
    }

    @Test
    public void stopSprint_sprintWithoutStartedAtGetsOnlyEndedAt() {

        //given
        Sprint activeSprint = prepareSprintWithIssuesToStop().sprint();

        //when
        sprintService.stopSprint(defaultScrumProject.getId());

        //then
        assertNull(activeSprint.getStartedAt());
        assertNotNull(activeSprint.getEndedAt());
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

    private static Issue createIssueOfProject(Project project) {

        Issue issue = createIssueEntity();
        issue.setProjectID(project.getId());
        return issue;
    }

    private Sprint saveSprintWithIssue() {

        Sprint sprint = sprintRepository.save(createSprintEntity(defaultScrumProject, Boolean.FALSE));
        Issue issue = issueRepository.save(createIssueEntityWithSprint(sprint));
        sprint.setSprintIssues(new HashSet<>(List.of(issue)));
        return sprintRepository.save(sprint);
    }

    // the database keeps timestamps with microsecond precision
    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static void assertBetween(Instant from, Instant actual, Instant to) {

        assertNotNull(actual);
        Instant value = actual.truncatedTo(ChronoUnit.MICROS);
        assertFalse(value.isBefore(from), () -> value + " is before " + from);
        assertFalse(value.isAfter(to.plus(1, ChronoUnit.MICROS)), () -> value + " is after " + to);
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
