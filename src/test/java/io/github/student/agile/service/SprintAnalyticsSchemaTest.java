package io.github.student.agile.service;

import io.github.student.agile.builder.IssueBuilder;
import io.github.student.agile.builder.SprintBuilder;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatusHistory;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueSnapshot;
import io.github.student.agile.resource.repository.SprintIssueSnapshotRepository;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.repository.BacklogRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusHistoryRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.SprintService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.List;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithSprint;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.FINISHED_SPRINT_CANNOT_BE_DELETED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NEGATIVE_ESTIMATION;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_END_DATE_BEFORE_START_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Covers the schema additions for the sprint analytics module (estimation rule, sprint end date, status history). */
@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class SprintAnalyticsSchemaTest {

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
    @Autowired
    private SprintIssueSnapshotRepository snapshotRepository;
    @Autowired
    private IssueService issueService;
    @Autowired
    private SprintService sprintService;

    private Project project;

    @BeforeEach
    public void setUp() {
        project = projectRepository.save(createProjectEntity());
        Backlog backlog = project.getBacklog();
        backlog.setProject(project);
        backlogRepository.save(backlog);
    }

    @Test
    public void changeIssueStatus_recordsHistoryRow() {
        Sprint sprint = sprintRepository.save(SprintBuilder.createSprintEntity(project, Boolean.TRUE));
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setIssueStatus(issueStatusRepository.findById(IssueStatusID.TO_DO.getStatusID()).orElseThrow());
        issueRepository.save(issue);

        issueService.changeIssueStatus(issue.getId(), IssueStatusID.IN_PROGRESS.getStatusID());

        List<IssueStatusHistory> rows = historyRepository.findAllByIssueIdOrderByChangedAtAsc(issue.getId());
        assertEquals(1, rows.size());
        assertEquals(sprint.getId(), rows.get(0).getSprintId());
        assertEquals(IssueStatusID.TO_DO.getStatusID(), rows.get(0).getOldStatus().getId());
        assertEquals(IssueStatusID.IN_PROGRESS.getStatusID(), rows.get(0).getNewStatus().getId());
        assertNotNull(rows.get(0).getChangedAt());
    }

    @Test
    public void changeIssueStatus_rejectedChangeWritesNoHistory() {
        Sprint sprint = sprintRepository.save(SprintBuilder.createSprintEntity(project, Boolean.TRUE));
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setIssueStatus(issueStatusRepository.findById(IssueStatusID.TO_DO.getStatusID()).orElseThrow());
        issueRepository.save(issue);

        // TO_DO -> DONE violates the status flow
        assertThrows(RuntimeException.class,
                () -> issueService.changeIssueStatus(issue.getId(), IssueStatusID.DONE.getStatusID()));

        assertEquals(0, historyRepository.findAllByIssueIdOrderByChangedAtAsc(issue.getId()).size());
    }

    @Test
    public void negativeEstimation_isRejected() {
        IssueMRO create = IssueBuilder.createManyIssues(1, project.getId()).get(0);
        create.setEstimation(-1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> issueService.createIssue(create));

        assertEquals(NEGATIVE_ESTIMATION, ex.getMessage());
    }

    @Test
    public void missingEstimation_isAllowed() {
        Sprint sprint = sprintRepository.save(SprintBuilder.createSprintEntity(project, Boolean.FALSE));
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setEstimation(null);

        Issue saved = issueRepository.saveAndFlush(issue);

        assertNull(saved.getEstimation());
    }

    @Test
    public void createSprint_endBeforeStart_isRejected() {
        ZonedDateTime start = ZonedDateTime.now();
        SprintDTO dto = SprintBuilder.createSprint(project.getId());
        dto.setStartTime(start);
        dto.setEndTime(start.minusDays(1));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> sprintService.createSprint(dto));

        assertEquals(SPRINT_END_DATE_BEFORE_START_DATE, ex.getMessage());
    }

    @Test
    public void stopSprint_snapshotsDoneAndUndoneIssuesWithEstimations() {
        Sprint sprint = SprintBuilder.createSprintEntity(project, Boolean.TRUE);
        sprint.setSprintIssues(new java.util.HashSet<>());
        sprint = sprintRepository.save(sprint);

        Issue done = createIssueEntityWithSprint(sprint);
        done.setEstimation(5);
        done.setIssueStatus(issueStatusRepository.findById(IssueStatusID.DONE.getStatusID()).orElseThrow());
        Issue open = createIssueEntityWithSprint(sprint);
        open.setEstimation(null);
        open.setIssueStatus(issueStatusRepository.findById(IssueStatusID.IN_PROGRESS.getStatusID()).orElseThrow());
        issueRepository.saveAll(List.of(done, open));
        sprint.getSprintIssues().addAll(List.of(done, open));
        sprintRepository.save(sprint);

        sprintService.stopSprint(project.getId());

        List<SprintIssueSnapshot> rows = snapshotRepository.findAllBySprintId(sprint.getId());
        assertEquals(2, rows.size());
        SprintIssueSnapshot doneRow = rows.stream().filter(r -> r.getIssueId().equals(done.getId())).findFirst().orElseThrow();
        SprintIssueSnapshot openRow = rows.stream().filter(r -> r.getIssueId().equals(open.getId())).findFirst().orElseThrow();
        assertEquals(5, doneRow.getEstimation());
        assertEquals(true, doneRow.getCompleted());
        assertNull(openRow.getEstimation());
        assertEquals(false, openRow.getCompleted());
        // status is captured before the unfinished issue is reset to TO_DO
        assertEquals(IssueStatusID.IN_PROGRESS.getStatusID(), openRow.getFinalStatus().getId());
        assertEquals(IssueStatusID.TO_DO.getStatusID(), open.getIssueStatus().getId());
    }

    @Test
    public void deleteSprint_finishedSprintIsRejected() {
        Sprint sprint = SprintBuilder.createSprintEntity(project, Boolean.FALSE);
        sprint.setIsDone(Boolean.TRUE);
        Sprint saved = sprintRepository.save(sprint);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> sprintService.deleteSprint(saved.getId()));

        assertEquals(FINISHED_SPRINT_CANNOT_BE_DELETED, ex.getMessage());
    }

    @Test
    public void createSprint_storesEndDate() {
        ZonedDateTime start = ZonedDateTime.now();
        SprintDTO dto = SprintBuilder.createSprint(project.getId());
        dto.setStartTime(start);
        dto.setEndTime(start.plusDays(14));

        SprintDTO created = sprintService.createSprint(dto);

        assertNotNull(created.getEndTime());
    }
}
