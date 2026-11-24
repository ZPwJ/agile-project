package io.github.student.agile.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SprintAnalyticsServiceComparisonTest {

    @Mock private SprintRepository sprintRepository;
    @Mock private IssueRepository issueRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private SprintMetricsCalculator metricsCalculator;

    @InjectMocks private SprintAnalyticsService analyticsService;

    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(10).name("Project").build();
    }

    @Test
    void missingProjectReturnsNotFound() {
        when(projectRepository.findById(10)).thenReturn(Optional.empty());
        assertThrows(AnalyticsNotFoundException.class, () -> analyticsService.compareSprints(10, null, null));
    }

    @Test
    void fromAfterToIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> analyticsService.compareSprints(10, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1)));
    }

    @Test
    void filtersByInclusiveStartDateAndSortsAscending() {
        when(projectRepository.findById(10)).thenReturn(Optional.of(project));

        Sprint early = sprint(1, "Early", ZonedDateTime.of(2024, 1, 10, 0, 0, 0, 0, ZoneOffset.UTC), false, false);
        Sprint late = sprint(2, "Late", ZonedDateTime.of(2024, 2, 10, 0, 0, 0, 0, ZoneOffset.UTC), false, false);
        Sprint withoutStart = sprint(3, "No start", null, false, false);
        when(sprintRepository.findAllByProject(project)).thenReturn(List.of(late, early, withoutStart));

        Issue doneIssue = issue(early, DONE.getStatusID(), 5);
        when(issueRepository.findAllBySprintIn(ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(doneIssue));
        when(metricsCalculator.calculate(early, List.of(doneIssue))).thenReturn(new SprintMetrics(1, 1, 0, 5, 0));

        List<SprintComparisonDTO> result = analyticsService.compareSprints(
                10, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().sprintId());
        assertEquals(1, result.getFirst().completedIssueCount());
        assertEquals(5, result.getFirst().velocity());
        assertEquals(SprintComparisonStatus.PLANNED, result.getFirst().status());
        assertEquals("STORY_POINTS", result.getFirst().estimationUnit());
    }

    @Test
    void loadsIssuesInSingleQueryForAllSprints() {
        when(projectRepository.findById(10)).thenReturn(Optional.of(project));
        Sprint sprint = sprint(1, "S1", null, false, false);
        when(sprintRepository.findAllByProject(project)).thenReturn(List.of(sprint));
        when(issueRepository.findAllBySprintIn(List.of(sprint))).thenReturn(List.of());
        when(metricsCalculator.calculate(sprint, List.of())).thenReturn(SprintMetrics.EMPTY);

        analyticsService.compareSprints(10, null, null);

        verify(issueRepository).findAllBySprintIn(List.of(sprint));
    }

    @Test
    void completedSprintUsesIncompleteSnapshot() {
        when(projectRepository.findById(10)).thenReturn(Optional.of(project));
        Sprint sprint = sprint(5, "Done", null, false, true);
        sprint.setIncompleteIssueCount(2);
        when(sprintRepository.findAllByProject(project)).thenReturn(List.of(sprint));
        when(issueRepository.findAllBySprintIn(List.of(sprint))).thenReturn(List.of());
        when(metricsCalculator.calculate(sprint, List.of())).thenReturn(SprintMetrics.EMPTY);

        SprintComparisonDTO row = analyticsService.compareSprints(10, null, null).getFirst();

        assertEquals(2, row.incompleteIssueCount());
        assertEquals(SprintComparisonStatus.DONE, row.status());
    }

    private static Sprint sprint(Integer id, String name, ZonedDateTime start, boolean active, boolean done) {
        return Sprint.builder()
                .id(id)
                .name(name)
                .startDate(start)
                .active(active)
                .isDone(done)
                .project(Project.builder().id(10).build())
                .build();
    }

    private static Issue issue(Sprint sprint, Integer statusId, Integer estimation) {
        return Issue.builder()
                .sprint(sprint)
                .estimation(estimation)
                .issueStatus(new IssueStatus(statusId, null))
                .build();
    }
}