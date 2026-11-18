package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.NullIdentifierException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.domain.dictionary.SprintEventType;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintAnalyticsServiceImpl implements SprintAnalyticsService {

    public static final String DEFAULT_ESTIMATION_UNIT = "STORY_POINTS";

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final SprintIssueEventRepository sprintIssueEventRepository;
    private final SprintVelocityCalculator sprintVelocityCalculator;
    private final UserService userService;

    @Override
    public SprintSummaryReportDTO getSprintSummaryReport(Integer sprintId) {
        if (sprintId == null) {
            throw new NullIdentifierException();
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException(SPRINT_DOES_NOT_EXISTS));

        Integer projectId = sprint.getProject().getId();
        userService.verifyProjectAccess(projectId);

        List<Issue> currentIssues = issueRepository.findAllBySprintId(sprintId);
        List<SprintIssueEvent> events = sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId);

        int initialIssueCount = resolveInitialIssueCount(events, currentIssues.size());
        int completedIssueCount = (int) currentIssues.stream()
                .filter(IssueStatusID::isCompleted)
                .count();
        int velocity = sprintVelocityCalculator.calculateVelocity(sprintId, currentIssues);

        int uncompletedIssueCount;
        int uncompletedEstimationSum;

        if (Boolean.TRUE.equals(sprint.getIsDone()) && !events.isEmpty()) {
            List<IssueSnapshot> uncompletedAtStop = resolveUncompletedSnapshotsAtStop(events);
            uncompletedIssueCount = uncompletedAtStop.size();
            uncompletedEstimationSum = uncompletedAtStop.stream()
                    .mapToInt(snapshot -> sprintVelocityCalculator.safeEstimation(snapshot.estimation()))
                    .sum();
        } else {
            List<Issue> uncompletedCurrent = currentIssues.stream()
                    .filter(issue -> !IssueStatusID.isCompleted(issue))
                    .toList();
            uncompletedIssueCount = uncompletedCurrent.size();
            uncompletedEstimationSum = uncompletedCurrent.stream()
                    .mapToInt(sprintVelocityCalculator::safeEstimation)
                    .sum();
        }

        return SprintSummaryReportDTO.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .projectId(projectId)
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .initialIssueCount(initialIssueCount)
                .completedIssueCount(completedIssueCount)
                .uncompletedIssueCount(uncompletedIssueCount)
                .velocity(velocity)
                .uncompletedEstimationSum(uncompletedEstimationSum)
                .estimationUnit(DEFAULT_ESTIMATION_UNIT)
                .build();
    }

    private int resolveInitialIssueCount(List<SprintIssueEvent> events, int fallbackCount) {
        int startedCount = (int) events.stream()
                .filter(event -> event.getEventType() == SprintEventType.SPRINT_STARTED)
                .count();
        return startedCount > 0 ? startedCount : fallbackCount;
    }

    private List<IssueSnapshot> resolveUncompletedSnapshotsAtStop(List<SprintIssueEvent> events) {
        Map<Integer, IssueSnapshot> scopeAtStop = new LinkedHashMap<>();

        for (SprintIssueEvent event : events) {
            if (event.getEventType() == SprintEventType.SPRINT_STOPPED) {
                break;
            }
            Integer issueId = event.getIssueId();
            if (issueId == null) {
                continue;
            }
            switch (event.getEventType()) {
                case SPRINT_STARTED, ADDED_TO_SPRINT, STATUS_CHANGED ->
                        scopeAtStop.put(issueId, new IssueSnapshot(event.getStatusId(), event.getEstimation()));
                case REMOVED_FROM_SPRINT -> scopeAtStop.remove(issueId);
                case ESTIMATION_CHANGED -> scopeAtStop.compute(issueId, (id, existing) -> new IssueSnapshot(
                        event.getStatusId() != null ? event.getStatusId() : (existing != null ? existing.statusId() : null),
                        event.getEstimation()
                ));
                case SPRINT_STOPPED -> {
                }
            }
        }

        return scopeAtStop.values().stream()
                .filter(snapshot -> !IssueStatusID.isCompleted(snapshot.statusId()))
                .toList();
    }

    private record IssueSnapshot(Integer statusId, Integer estimation) {
    }
}
