package io.github.student.agile.analytics;

import io.github.student.agile.builder.SprintBuilder;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.analytics.error.SprintNotFoundException;
import io.github.student.agile.resource.analytics.metrics.SprintMetrics;
import io.github.student.agile.resource.analytics.metrics.SprintMetricsProvider;
import io.github.student.agile.resource.analytics.metrics.SprintState;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.repository.BacklogRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.SprintService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithSprint;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class SprintMetricsServiceTest {

    @Autowired
    private SprintMetricsProvider provider;
    @Autowired
    private SprintService sprintService;
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

    private Project project;

    @BeforeEach
    public void setUp() {
        project = projectRepository.save(createProjectEntity());
        Backlog backlog = project.getBacklog();
        backlog.setProject(project);
        backlogRepository.save(backlog);
    }

    private Sprint sprint(boolean active, boolean done) {
        Sprint s = SprintBuilder.createSprintEntity(project, active);
        s.setIsDone(done);
        s.setSprintIssues(new HashSet<>());
        return sprintRepository.save(s);
    }

    private Issue issue(Sprint sprint, Integer estimation, IssueStatusID status) {
        Issue i = createIssueEntityWithSprint(sprint);
        i.setEstimation(estimation);
        i.setIssueStatus(issueStatusRepository.findById(status.getStatusID()).orElseThrow());
        i = issueRepository.save(i);
        sprint.getSprintIssues().add(i);
        return i;
    }

    @Test
    public void emptySprint_returnsZeros() {
        Sprint s = sprint(false, false);

        SprintMetrics m = provider.forSprint(s.getId());

        assertEquals(SprintState.PLANNED, m.state());
        assertEquals(0, m.totalTasks());
        assertEquals(0, m.completedEstimate());
    }

    @Test
    public void activeSprint_isReadFromLiveIssues() {
        Sprint s = sprint(true, false);
        issue(s, 5, IssueStatusID.DONE);
        issue(s, 3, IssueStatusID.IN_PROGRESS);
        issue(s, null, IssueStatusID.TO_DO);

        SprintMetrics m = provider.forSprint(s.getId());

        assertEquals(SprintState.ACTIVE, m.state());
        assertEquals(3, m.totalTasks());
        assertEquals(1, m.completedTasks());
        assertEquals(2, m.incompleteTasks());
        assertEquals(1, m.unestimatedTasks());
        assertEquals(5, m.completedEstimate());
        assertEquals(3, m.incompleteEstimate());
    }

    @Test
    public void finishedSprint_isReadFromSnapshotNotFromLiveIssues() {
        Sprint s = sprint(true, false);
        issue(s, 8, IssueStatusID.DONE);
        issue(s, 2, IssueStatusID.IN_TESTS);
        Integer sprintId = s.getId();

        sprintService.stopSprint(project.getId());
        // after stopping, issues are detached: a live read would give 0 tasks
        SprintMetrics m = provider.forSprint(sprintId);

        assertEquals(SprintState.FINISHED, m.state());
        assertEquals(2, m.totalTasks());
        assertEquals(1, m.completedTasks());
        assertEquals(8, m.completedEstimate());
        assertEquals(2, m.incompleteEstimate());
    }

    @Test
    public void sprintsAreNeverMixed() {
        Sprint a = sprint(true, false);
        Sprint b = sprint(false, false);
        issue(a, 5, IssueStatusID.DONE);
        issue(b, 7, IssueStatusID.DONE);

        Map<Integer, SprintMetrics> result = provider.forSprints(List.of(a.getId(), b.getId()));

        assertEquals(5, result.get(a.getId()).completedEstimate());
        assertEquals(7, result.get(b.getId()).completedEstimate());
        assertEquals(1, result.get(a.getId()).totalTasks());
    }

    @Test
    public void batch_keepsRequestedOrderAndIgnoresDuplicates() {
        Sprint a = sprint(false, false);
        Sprint b = sprint(false, false);

        Map<Integer, SprintMetrics> result = provider.forSprints(List.of(b.getId(), a.getId(), b.getId()));

        assertEquals(List.of(b.getId(), a.getId()), List.copyOf(result.keySet()));
    }

    @Test
    public void batch_withNoIds_returnsEmptyMap() {
        assertTrue(provider.forSprints(List.of()).isEmpty());
    }

    @Test
    public void unknownSprint_throwsSprintNotFound() {
        assertThrows(SprintNotFoundException.class, () -> provider.forSprint(-1));
    }

    @Test
    public void batch_withOneUnknownSprint_throwsSprintNotFound() {
        Sprint a = sprint(false, false);

        assertThrows(SprintNotFoundException.class, () -> provider.forSprints(List.of(a.getId(), -1)));
    }

    @Test
    public void repeatedCalls_giveSameResult() {
        Sprint s = sprint(true, false);
        issue(s, 5, IssueStatusID.DONE);

        assertEquals(provider.forSprint(s.getId()), provider.forSprint(s.getId()));
    }
}
