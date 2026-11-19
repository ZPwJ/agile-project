package io.github.student.agile.resource.analytics.metrics;

import io.github.student.agile.resource.analytics.error.SprintNotFoundException;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueSnapshot;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.SprintIssueSnapshotRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads sprint data and delegates counting to {@link SprintMetricsCalculator}.
 * Finished sprints are read from the stop-time snapshot (their issues are detached);
 * planned and active sprints are read from live issues. Each source costs one query for any number of sprints.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SprintMetricsService implements SprintMetricsProvider {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final SprintIssueSnapshotRepository snapshotRepository;
    private final SprintCompletionPolicy completionPolicy;

    @Override
    public SprintMetrics forSprint(Integer sprintId) {
        return forSprints(List.of(sprintId)).get(sprintId);
    }

    @Override
    public Map<Integer, SprintMetrics> forSprints(Collection<Integer> sprintIds) {
        Set<Integer> ids = new LinkedHashSet<>(sprintIds);
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Sprint> sprints = sprintRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Sprint::getId, Function.identity()));
        ids.stream().filter(id -> !sprints.containsKey(id)).findFirst().ifPresent(id -> {
            throw new SprintNotFoundException(id);
        });

        List<Sprint> finished = sprints.values().stream().filter(s -> stateOf(s) == SprintState.FINISHED).toList();
        List<Sprint> open = sprints.values().stream().filter(s -> stateOf(s) != SprintState.FINISHED).toList();

        Map<Integer, List<IssueOutcome>> outcomes = new HashMap<>();
        if (!finished.isEmpty()) {
            for (SprintIssueSnapshot row : snapshotRepository.findAllBySprintIdIn(idsOf(finished))) {
                outcomes.computeIfAbsent(row.getSprint().getId(), k -> new ArrayList<>())
                        .add(new IssueOutcome(row.getEstimation(), Boolean.TRUE.equals(row.getCompleted())));
            }
        }
        if (!open.isEmpty()) {
            for (Issue issue : issueRepository.findAllBySprintIn(open)) {
                outcomes.computeIfAbsent(issue.getSprint().getId(), k -> new ArrayList<>())
                        .add(new IssueOutcome(issue.getEstimation(), completionPolicy.isCompleted(issue.getIssueStatus())));
            }
        }

        Map<Integer, SprintMetrics> result = new LinkedHashMap<>();
        for (Integer id : ids) {
            result.put(id, SprintMetricsCalculator.calculate(
                    id, stateOf(sprints.get(id)), outcomes.getOrDefault(id, List.of())));
        }
        return result;
    }

    /** Derived from the sprint flags: done -> FINISHED, active -> ACTIVE, otherwise PLANNED. */
    public static SprintState stateOf(Sprint sprint) {
        if (Boolean.TRUE.equals(sprint.getIsDone())) {
            return SprintState.FINISHED;
        }
        return Boolean.TRUE.equals(sprint.getActive()) ? SprintState.ACTIVE : SprintState.PLANNED;
    }

    private static List<Integer> idsOf(List<Sprint> sprints) {
        return sprints.stream().map(Sprint::getId).toList();
    }
}
