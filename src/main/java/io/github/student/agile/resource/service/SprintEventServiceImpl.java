package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.SprintEventType;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.lang.Boolean.TRUE;

@Service
@Transactional
@RequiredArgsConstructor
public class SprintEventServiceImpl implements SprintEventService {

    private final SprintIssueEventRepository sprintIssueEventRepository;

    @Override
    public void recordSprintStarted(Sprint sprint, ZonedDateTime occurredAt) {
        if (sprint == null || sprint.getId() == null || sprint.getSprintIssues() == null) {
            return;
        }
        ZonedDateTime eventTime = occurredAt != null ? occurredAt : ZonedDateTime.now();
        List<SprintIssueEvent> initialEvents = sprint.getSprintIssues().stream()
                .sorted(Comparator.comparing(Issue::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(issue -> SprintIssueEvent.builder()
                        .sprintId(sprint.getId())
                        .issueId(issue.getId())
                        .eventType(SprintEventType.SPRINT_STARTED)
                        .statusId(extractStatusId(issue.getIssueStatus()))
                        .estimation(issue.getEstimation())
                        .occurredAt(eventTime)
                        .build())
                .toList();
        sprintIssueEventRepository.saveAll(initialEvents);
    }

    @Override
    public void recordIssueAddedToSprint(Sprint sprint, Issue issue) {
        if (!isActiveSprint(sprint) || issue == null) {
            return;
        }
        sprintIssueEventRepository.save(SprintIssueEvent.builder()
                .sprintId(sprint.getId())
                .issueId(issue.getId())
                .eventType(SprintEventType.ADDED_TO_SPRINT)
                .statusId(extractStatusId(issue.getIssueStatus()))
                .estimation(issue.getEstimation())
                .occurredAt(ZonedDateTime.now())
                .build());
    }

    @Override
    public void recordIssueRemovedFromSprint(Sprint sprint, Issue issue) {
        if (!isActiveSprint(sprint) || issue == null) {
            return;
        }
        sprintIssueEventRepository.save(SprintIssueEvent.builder()
                .sprintId(sprint.getId())
                .issueId(issue.getId())
                .eventType(SprintEventType.REMOVED_FROM_SPRINT)
                .statusId(extractStatusId(issue.getIssueStatus()))
                .estimation(issue.getEstimation())
                .occurredAt(ZonedDateTime.now())
                .build());
    }

    @Override
    public void recordStatusChanged(Issue issue, IssueStatus previousStatus, IssueStatus newStatus) {
        if (issue == null || !isActiveSprint(issue.getSprint())) {
            return;
        }
        Integer previousStatusId = extractStatusId(previousStatus);
        Integer newStatusId = extractStatusId(newStatus);
        if (Objects.equals(previousStatusId, newStatusId)) {
            return;
        }
        sprintIssueEventRepository.save(SprintIssueEvent.builder()
                .sprintId(issue.getSprint().getId())
                .issueId(issue.getId())
                .eventType(SprintEventType.STATUS_CHANGED)
                .previousStatusId(previousStatusId)
                .statusId(newStatusId)
                .estimation(issue.getEstimation())
                .occurredAt(ZonedDateTime.now())
                .build());
    }

    @Override
    public void recordEstimationChanged(Issue issue, Integer previousEstimation, Integer newEstimation) {
        if (issue == null || !isActiveSprint(issue.getSprint())) {
            return;
        }
        if (Objects.equals(previousEstimation, newEstimation)) {
            return;
        }
        sprintIssueEventRepository.save(SprintIssueEvent.builder()
                .sprintId(issue.getSprint().getId())
                .issueId(issue.getId())
                .eventType(SprintEventType.ESTIMATION_CHANGED)
                .statusId(extractStatusId(issue.getIssueStatus()))
                .previousEstimation(previousEstimation)
                .estimation(newEstimation)
                .occurredAt(ZonedDateTime.now())
                .build());
    }

    @Override
    public void recordSprintStopped(Sprint sprint, ZonedDateTime occurredAt) {
        if (sprint == null || sprint.getId() == null) {
            return;
        }
        ZonedDateTime eventTime = occurredAt != null ? occurredAt : ZonedDateTime.now();
        sprintIssueEventRepository.save(SprintIssueEvent.builder()
                .sprintId(sprint.getId())
                .eventType(SprintEventType.SPRINT_STOPPED)
                .occurredAt(eventTime)
                .build());
    }

    @Override
    public void deleteEventsForSprint(Integer sprintId) {
        if (sprintId != null) {
            sprintIssueEventRepository.deleteAllBySprintId(sprintId);
        }
    }

    private boolean isActiveSprint(Sprint sprint) {
        return sprint != null && sprint.getId() != null && TRUE.equals(sprint.getActive());
    }

    private Integer extractStatusId(IssueStatus status) {
        return status != null ? status.getId() : null;
    }
}
