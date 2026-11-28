package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.SprintIssueEventType;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.ADDED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.COMMITTED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.COMPLETED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.REMOVED;

/**
 * The only place that creates sprint events. It always joins the transaction of the change it describes.
 * <p>
 * An issue keeps one estimation for as long as it stays in the scope: REMOVED and COMPLETED copy the value
 * of the last COMMITTED or ADDED event. Thanks to that the completed work never exceeds the scope,
 * even if the estimation of the issue is changed during the sprint.
 */
@Component
@Transactional(Transactional.TxType.MANDATORY)
@RequiredArgsConstructor
public class SprintEventRecorder {

    private static final List<SprintIssueEventType> SCOPE_ENTRY_TYPES = List.of(COMMITTED, ADDED);

    private final SprintIssueEventRepository sprintIssueEventRepository;

    public void recordCommitted(Integer sprintID, Collection<Issue> issues, Instant startedAt) {

        sprintIssueEventRepository.saveAll(issues.stream()
                .map(issue -> event(sprintID, issue, COMMITTED, issue.getEstimation(), startedAt))
                .toList());
    }

    public void recordAdded(Integer sprintID, Issue issue) {
        sprintIssueEventRepository.save(event(sprintID, issue, ADDED, issue.getEstimation(), Instant.now()));
    }

    public void recordRemoved(Integer sprintID, Issue issue) {
        sprintIssueEventRepository.save(event(sprintID, issue, REMOVED, estimationInScope(sprintID, issue), Instant.now()));
    }

    public void recordCompleted(Integer sprintID, Issue issue) {
        sprintIssueEventRepository.save(event(sprintID, issue, COMPLETED, estimationInScope(sprintID, issue), Instant.now()));
    }

    private Integer estimationInScope(Integer sprintID, Issue issue) {

        Optional<SprintIssueEvent> scopeEntry = sprintIssueEventRepository
                .findFirstBySprintIDAndIssueIDAndTypeInOrderByOccurredAtDescIdDesc(sprintID, issue.getId(), SCOPE_ENTRY_TYPES);

        // not Optional.map: an entry without estimation must stay without estimation
        if (scopeEntry.isPresent()) {
            return scopeEntry.get().getEstimation();
        }
        // no entry: the sprint was started before the history was introduced
        return issue.getEstimation();
    }

    private SprintIssueEvent event(Integer sprintID, Issue issue, SprintIssueEventType type,
                                   Integer estimation, Instant occurredAt) {
        return SprintIssueEvent.builder()
                .sprintID(sprintID)
                .issueID(issue.getId())
                .type(type)
                .estimation(estimation)
                .occurredAt(occurredAt)
                .build();
    }
}
