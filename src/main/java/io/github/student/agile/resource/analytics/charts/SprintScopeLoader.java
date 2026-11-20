package io.github.student.agile.resource.analytics.charts;

import io.github.student.agile.resource.analytics.metrics.SprintCompletionPolicy;
import io.github.student.agile.resource.analytics.metrics.SprintState;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.SprintIssueSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Loads the tasks of one sprint at chart granularity, from the same sources as
 * {@code SprintMetricsService}: the stop-time snapshot for a finished sprint, live issues otherwise.
 * Daily points need each estimation, which the aggregate metrics do not expose.
 */
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintScopeLoader {

    private final SprintIssueSnapshotRepository snapshotRepository;
    private final IssueRepository issueRepository;
    private final SprintCompletionPolicy completionPolicy;

    /** Exactly one query per call: snapshot rows or live issues, never both and never one per task. */
    public List<SprintTaskPoints> load(Sprint sprint, SprintState state) {
        if (state == SprintState.FINISHED) {
            return snapshotRepository.findAllBySprintId(sprint.getId()).stream()
                    .map(row -> new SprintTaskPoints(row.getIssueId(), row.getEstimation(),
                            Boolean.TRUE.equals(row.getCompleted())))
                    .toList();
        }
        return issueRepository.findAllBySprint(sprint).stream()
                .map(issue -> new SprintTaskPoints(issue.getId(), issue.getEstimation(),
                        completionPolicy.isCompleted(issue.getIssueStatus())))
                .toList();
    }
}
