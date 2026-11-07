package io.github.student.agile.resource.domain;

import io.github.student.agile.resource.domain.dictionary.SprintIssueEventType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Append-only history of a started sprint, used by the analytics reports.
 * IDs are plain values, not relations, so the history survives issue deletion.
 */
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class SprintIssueEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sprint_issue_event_seq_gen")
    @SequenceGenerator(name = "sprint_issue_event_seq_gen", sequenceName = "sprint_issue_event_seq")
    private Integer id;

    private Integer sprintID;

    private Integer issueID;

    @Enumerated(EnumType.STRING)
    private SprintIssueEventType type;

    private Integer estimation;

    private ZonedDateTime occurredAt;

    public static SprintIssueEvent of(Sprint sprint, Issue issue, SprintIssueEventType type) {
        return new SprintIssueEvent(null, sprint.getId(), issue.getId(), type, issue.getEstimation(), ZonedDateTime.now());
    }
}
