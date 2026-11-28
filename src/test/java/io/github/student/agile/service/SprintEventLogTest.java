package io.github.student.agile.service;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.domain.dictionary.SprintIssueEventType;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.SprintEventReader;
import io.github.student.agile.resource.service.SprintService;
import io.github.student.agile.resource.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntity;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.CODE_REVIEW;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_TESTS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.ADDED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.COMMITTED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.COMPLETED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.REMOVED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestConfiguration.class, properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
public class SprintEventLogTest {

    @Autowired
    private SprintService sprintService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private SprintEventReader sprintEventReader;

    @Autowired
    private SprintIssueEventRepository sprintIssueEventRepository;

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

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @MockBean
    private UserService userService;

    private Project project;

    @BeforeEach
    void setUp() {

        Mockito.when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        project = projectRepository.save(createProjectEntity());
    }

    @Test
    public void startSprint_recordsCommittedForEveryIssue() {

        //given
        Integer sprintID = createPlannedSprint();
        Integer estimated = createIssue(5);
        Integer notEstimated = createIssue(null);
        add(sprintID, estimated);
        add(sprintID, notEstimated);

        //when
        start(sprintID);

        //then
        List<SprintIssueEvent> events = events(sprintID);
        assertEquals(2, events.size());
        assertEvent(events, estimated, COMMITTED, 5);
        assertEvent(events, notEstimated, COMMITTED, null);
        Instant startedAt = sprintRepository.findById(sprintID).orElseThrow().getStartedAt();
        events.forEach(event -> assertEquals(micros(startedAt), micros(event.getOccurredAt())));
        assertTrue(events.get(0).getId() < events.get(1).getId());
    }

    @Test
    public void startSprint_rejectedStartRecordsNothing() {

        //given
        startedSprintWith(createIssue(1));
        Integer secondSprintID = createPlannedSprint();
        add(secondSprintID, createIssue(2));

        //when
        assertThrows(RuntimeException.class, () -> sprintService.startSprint(secondSprintID));

        //then
        assertTrue(events(secondSprintID).isEmpty());
    }

    @Test
    public void planningBeforeStartRecordsNothing() {

        //given
        Integer sprintID = createPlannedSprint();
        Integer issueID = createIssue(3);

        //when
        add(sprintID, issueID);
        remove(sprintID, issueID);

        //then
        assertTrue(events(sprintID).isEmpty());
    }

    @Test
    public void addIssueToActiveSprint_recordsAdded() {

        //given
        Integer sprintID = startedSprintWith(createIssue(1));
        Integer issueID = createIssue(8);

        //when
        add(sprintID, issueID);

        //then
        assertEquals(List.of("COMMITTED:1", "ADDED:8"), describe(sprintID));
    }

    @Test
    public void rejectedAddRecordsNothing() {

        //given
        Integer sprintID = startedSprintWith(createIssue(1));
        Project otherProject = projectRepository.save(createProjectEntity());
        Integer foreignIssueID = createIssue(2, TO_DO, otherProject.getId());
        Integer doneIssueID = createIssue(3, DONE, project.getId());

        //when
        assertThrows(RuntimeException.class, () -> sprintService.addIssueToSprint(foreignIssueID, sprintID));
        assertThrows(RuntimeException.class, () -> sprintService.addIssueToSprint(doneIssueID, sprintID));

        //then
        assertEquals(List.of("COMMITTED:1"), describe(sprintID));
    }

    @Test
    public void deleteIssueFromActiveSprint_recordsRemoved() {

        //given
        Integer issueID = createIssue(5);
        Integer sprintID = startedSprintWith(issueID);

        //when
        remove(sprintID, issueID);

        //then
        assertEquals(List.of("COMMITTED:5", "REMOVED:5"), describe(sprintID));
    }

    @Test
    public void deleteIssueFromSprint_recordsRemovedForTheSprintOfTheIssue() {

        //given
        Integer issueID = createIssue(5);
        Integer activeSprintID = startedSprintWith(issueID);
        Integer otherSprintID = createPlannedSprint();

        //when
        remove(otherSprintID, issueID);

        //then
        assertEquals(List.of("COMMITTED:5", "REMOVED:5"), describe(activeSprintID));
        assertTrue(events(otherSprintID).isEmpty());
    }

    @Test
    public void changeIssueStatus_recordsCompletedOnlyForDone() {

        //given
        Integer issueID = createIssue(5);
        Integer sprintID = startedSprintWith(issueID);

        //when
        changeStatus(issueID, IN_PROGRESS);
        changeStatus(issueID, CODE_REVIEW);
        changeStatus(issueID, IN_TESTS);
        List<String> beforeDone = describe(sprintID);
        changeStatus(issueID, DONE);

        //then
        assertEquals(List.of("COMMITTED:5"), beforeDone);
        assertEquals(List.of("COMMITTED:5", "COMPLETED:5"), describe(sprintID));
    }

    @Test
    public void changeIssueStatus_rejectedChangeRecordsNothing() {

        //given
        Integer issueID = createIssue(5);
        Integer sprintID = startedSprintWith(issueID);

        //when
        assertThrows(RuntimeException.class, () -> changeStatus(issueID, DONE));

        //then
        assertEquals(List.of("COMMITTED:5"), describe(sprintID));
    }

    @Test
    public void completed_copiesEstimationFromScopeEntry() {

        //given
        Integer issueID = createIssue(3);
        Integer sprintID = startedSprintWith(issueID);
        setEstimation(issueID, 8);

        //when
        complete(issueID);

        //then
        assertEquals(List.of("COMMITTED:3", "COMPLETED:3"), describe(sprintID));
    }

    @Test
    public void removed_keepsEmptyEstimationOfScopeEntry() {

        //given
        Integer issueID = createIssue(null);
        Integer sprintID = startedSprintWith(issueID);
        setEstimation(issueID, 5);

        //when
        remove(sprintID, issueID);

        //then
        assertEquals(List.of("COMMITTED:null", "REMOVED:null"), describe(sprintID));
    }

    @Test
    public void completed_usesTheLastScopeEntryAfterIssueIsAddedAgain() {

        //given
        Integer issueID = createIssue(3);
        Integer sprintID = startedSprintWith(issueID);
        remove(sprintID, issueID);
        setEstimation(issueID, 5);
        add(sprintID, issueID);
        setEstimation(issueID, 13);

        //when
        complete(issueID);

        //then
        assertEquals(List.of("COMMITTED:3", "REMOVED:3", "ADDED:5", "COMPLETED:5"), describe(sprintID));
    }

    @Test
    public void completed_withoutScopeEntryUsesCurrentEstimation() {

        //given a sprint started before the history was introduced
        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.TRUE));
        Issue issue = issueRepository.findById(createIssue(8)).orElseThrow();
        issue.setSprint(sprint);
        issueRepository.save(issue);
        flushAndClear();

        //when
        complete(issue.getId());

        //then
        assertEquals(List.of("COMPLETED:8"), describe(sprint.getId()));
    }

    @Test
    public void stopSprint_doesNotRecordEvents() {

        //given
        Integer doneIssueID = createIssue(3);
        Integer openIssueID = createIssue(5);
        Integer sprintID = createPlannedSprint();
        add(sprintID, doneIssueID);
        add(sprintID, openIssueID);
        start(sprintID);
        complete(doneIssueID);
        List<Integer> eventsBefore = events(sprintID).stream().map(SprintIssueEvent::getId).toList();

        //when
        sprintService.stopSprint(project.getId());
        flushAndClear();

        //then
        assertEquals(3, eventsBefore.size());
        assertEquals(eventsBefore, events(sprintID).stream().map(SprintIssueEvent::getId).toList());
    }

    @Test
    public void deleteIssue_inActiveSprintRecordsRemovedAndKeepsHistory() {

        //given
        Integer issueID = createIssue(5);
        Integer sprintID = startedSprintWith(issueID);
        setEstimation(issueID, 8);

        //when
        issueService.deleteIssue(issueID);
        flushAndClear();

        //then
        assertFalse(issueRepository.existsById(issueID));
        assertEquals(List.of("COMMITTED:5", "REMOVED:5"), describe(sprintID));
    }

    @Test
    public void deleteIssue_outsideActiveSprintRecordsNothing() {

        //given
        Integer sprintID = createPlannedSprint();
        Integer plannedIssueID = createIssue(5);
        add(sprintID, plannedIssueID);
        Integer backlogIssueID = createIssue(3);
        long eventsBefore = sprintIssueEventRepository.count();

        //when
        issueService.deleteIssue(plannedIssueID);
        issueService.deleteIssue(backlogIssueID);
        flushAndClear();

        //then
        assertEquals(eventsBefore, sprintIssueEventRepository.count());
    }

    @Test
    public void deleteSprint_finishedSprintWithHistoryCanBeDeleted() {

        //given
        Integer sprintID = startedSprintWith(createIssue(5));
        sprintService.stopSprint(project.getId());
        flushAndClear();

        //when
        sprintService.deleteSprint(sprintID);
        flushAndClear();

        //then
        assertFalse(sprintRepository.existsById(sprintID));
        assertEquals(List.of("COMMITTED:5"), describe(sprintID));
    }

    @Test
    public void reader_returnsOnlyRequestedSprintsInOrderWithOneStatement() {

        //given
        Instant time = Instant.parse("2026-01-01T10:00:00Z");
        SprintIssueEvent late = saveEvent(101, 1, time.plus(1, ChronoUnit.HOURS));
        SprintIssueEvent early = saveEvent(102, 2, time);
        SprintIssueEvent sameTime = saveEvent(101, 3, time);
        saveEvent(103, 4, time);
        flushAndClear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        //when
        List<SprintIssueEvent> events = sprintEventReader.findBySprintIDs(List.of(101, 102));

        //then
        assertEquals(1, statistics.getPrepareStatementCount());
        assertEquals(List.of(early.getId(), sameTime.getId(), late.getId()),
                events.stream().map(SprintIssueEvent::getId).toList());
    }

    @Test
    public void reader_returnsEmptyListWhenThereAreNoEvents() {

        assertTrue(sprintEventReader.findBySprintIDs(List.of()).isEmpty());
        assertTrue(sprintEventReader.findBySprintIDs(List.of(-10)).isEmpty());
    }

    private Integer createPlannedSprint() {

        Integer sprintID = sprintRepository.save(createSprintEntity(project, Boolean.FALSE)).getId();
        flushAndClear();
        return sprintID;
    }

    private Integer startedSprintWith(Integer issueID) {

        Integer sprintID = createPlannedSprint();
        add(sprintID, issueID);
        start(sprintID);
        return sprintID;
    }

    private Integer createIssue(Integer estimation) {
        return createIssue(estimation, TO_DO, project.getId());
    }

    private Integer createIssue(Integer estimation, IssueStatusID status, Integer projectID) {

        Issue issue = createIssueEntity();
        issue.setEstimation(estimation);
        issue.setProjectID(projectID);
        issue.setIssueStatus(issueStatusRepository.findById(status.getStatusID()).orElseThrow());
        Integer issueID = issueRepository.save(issue).getId();
        flushAndClear();
        return issueID;
    }

    private void add(Integer sprintID, Integer issueID) {
        sprintService.addIssueToSprint(issueID, sprintID);
        flushAndClear();
    }

    private void remove(Integer sprintID, Integer issueID) {
        sprintService.deleteIssueFromSprint(sprintID, issueID);
        flushAndClear();
    }

    private void start(Integer sprintID) {
        sprintService.startSprint(sprintID);
        flushAndClear();
    }

    private void changeStatus(Integer issueID, IssueStatusID status) {
        issueService.changeIssueStatus(issueID, status.getStatusID());
        flushAndClear();
    }

    private void complete(Integer issueID) {
        List.of(IN_PROGRESS, CODE_REVIEW, IN_TESTS, DONE).forEach(status -> changeStatus(issueID, status));
    }

    private void setEstimation(Integer issueID, Integer estimation) {

        issueRepository.findById(issueID).orElseThrow().setEstimation(estimation);
        flushAndClear();
    }

    private SprintIssueEvent saveEvent(Integer sprintID, Integer issueID, Instant occurredAt) {

        return sprintIssueEventRepository.save(SprintIssueEvent.builder()
                .sprintID(sprintID)
                .issueID(issueID)
                .type(COMMITTED)
                .occurredAt(occurredAt)
                .build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private List<SprintIssueEvent> events(Integer sprintID) {
        return sprintEventReader.findBySprintIDs(List.of(sprintID));
    }

    private List<String> describe(Integer sprintID) {
        return events(sprintID).stream()
                .map(event -> event.getType() + ":" + event.getEstimation())
                .toList();
    }

    private static void assertEvent(List<SprintIssueEvent> events, Integer issueID,
                                    SprintIssueEventType type, Integer estimation) {

        SprintIssueEvent found = events.stream()
                .filter(event -> event.getIssueID().equals(issueID))
                .findFirst()
                .orElseThrow();
        assertEquals(type, found.getType());
        assertEquals(estimation, found.getEstimation());
    }

    private static Instant micros(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }
}
