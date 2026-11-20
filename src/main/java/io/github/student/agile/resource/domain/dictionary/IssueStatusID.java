package io.github.student.agile.resource.domain.dictionary;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public enum IssueStatusID {
    TO_DO(1),
    IN_PROGRESS(2),
    CODE_REVIEW(3),
    IN_TESTS(4),
    DONE(5);

    /**
     * Statusy uznawane w systemie za stan końcowy (ukończenie zadania w sprincie).
     */
    public static final Set<IssueStatusID> COMPLETED_STATUSES = Set.of(DONE);

    private final Integer statusID;

    public static boolean isCompleted(Integer statusId) {
        return statusId != null
                && COMPLETED_STATUSES.stream()
                .anyMatch(completed -> completed.getStatusID().equals(statusId));
    }

    public static boolean isCompleted(IssueStatus status) {
        return status != null && isCompleted(status.getId());
    }

    public static boolean isCompleted(Issue issue) {
        return issue != null && isCompleted(issue.getIssueStatus());
    }
}
