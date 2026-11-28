package io.github.student.agile.resource.domain;

import io.github.student.agile.resource.domain.dictionary.SprintIssueEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * One fact from the history of a sprint. Events are never modified.
 * Sprint and issue are kept as plain identifiers, without foreign keys,
 * so that the history does not block deleting them.
 */
@Getter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "sprint_issue_event_sprint_issue_idx", columnList = "sprintID, issueID"))
public class SprintIssueEvent implements Serializable {

    @Id
    @Column(name = "SprintIssueEventID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sprint_issue_event_seq_gen")
    @SequenceGenerator(name = "sprint_issue_event_seq_gen", sequenceName = "sprint_issue_event_seq")
    private Integer id;

    @Column(nullable = false)
    private Integer sprintID;

    @Column(nullable = false)
    private Integer issueID;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SprintIssueEventType type;

    private Integer estimation;

    @Column(nullable = false)
    private Instant occurredAt;
}
