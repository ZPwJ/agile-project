package io.github.student.agile.resource.analytics.metrics;

import io.github.student.agile.resource.domain.IssueStatus;
import org.springframework.stereotype.Component;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;

/**
 * Defines which issue statuses count as "completed" for analytics.
 * Only {@code DONE} does: it is the terminal status of the existing flow
 * (TO_DO -> IN_PROGRESS -> CODE_REVIEW -> IN_TESTS -> DONE). Change it here only.
 */
@Component
public class SprintCompletionPolicy {

    public boolean isCompleted(IssueStatus status) {
        return status != null && DONE.getStatusID().equals(status.getId());
    }
}
