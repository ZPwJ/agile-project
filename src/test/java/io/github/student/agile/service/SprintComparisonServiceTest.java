package io.github.student.agile.service;

import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.dto.SprintReportDTO;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintProgressRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.SprintReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SprintComparisonServiceTest {

    @Mock private SprintRepository sprints;
    @Mock private SprintProgressRepository progress;
    @Mock private ProjectRepository projects;
    @InjectMocks private SprintReportService service;

    @Test
    void loadsHistoryInOneBatchAndIncludesSprintWithZeroVelocity() {
        Project project = Project.builder().id(1).build();
        ZonedDateTime end = ZonedDateTime.parse("2026-11-10T10:00:00Z");
        Sprint first = Sprint.builder().id(1).project(project).isDone(true).endDate(end).build();
        Sprint second = Sprint.builder().id(2).project(project).isDone(true).endDate(end).build();
        List<SprintProgress.Kind> kinds = List.of(SprintProgress.Kind.START, SprintProgress.Kind.END);
        when(projects.existsById(1)).thenReturn(true);
        when(sprints.findAllByProjectIdAndIsDoneTrue(1)).thenReturn(List.of(second, first));
        when(progress.findAllBySprintIdInAndKindInOrderByCapturedAtAscIdAsc(List.of(1, 2), kinds))
                .thenReturn(List.of(
                        SprintProgress.builder().sprint(first).kind(SprintProgress.Kind.END)
                                .issueCount(2).totalPoints(5).build(),
                        SprintProgress.builder().sprint(second).kind(SprintProgress.Kind.END)
                                .issueCount(2).completedIssueCount(1).totalPoints(8).completedPoints(3).build()));

        List<SprintReportDTO> result = service.getComparison(1, null, null);

        assertThat(result).extracting(SprintReportDTO::sprintID).containsExactly(1, 2);
        assertThat(result).extracting(SprintReportDTO::velocity).containsExactly(0L, 3L);
        assertThat(result).extracting(SprintReportDTO::incompleteIssueCount).containsExactly(2, 1);
        verify(progress).findAllBySprintIdInAndKindInOrderByCapturedAtAscIdAsc(List.of(1, 2), kinds);
        verify(progress, never()).findAllBySprintIdOrderByCapturedAtAscIdAsc(any());
        verify(sprints, never()).findById(any());
    }

    @Test
    void emptyProjectDoesNotQueryHistory() {
        when(projects.existsById(1)).thenReturn(true);
        when(sprints.findAllByProjectIdAndIsDoneTrue(1)).thenReturn(List.of());

        assertThat(service.getComparison(1, null, null)).isEmpty();
        verifyNoInteractions(progress);
    }

    @Test
    void missingProjectDoesNotQuerySprintsOrHistory() {
        assertThatThrownBy(() -> service.getComparison(1, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(sprints, progress);
    }
}
