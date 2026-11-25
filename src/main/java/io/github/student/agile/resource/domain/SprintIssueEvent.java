package io.github.student.agile.resource.domain;

import io.github.student.agile.resource.domain.dictionary.SprintEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * An entry in the sprint history: a scope change, an estimation change or an issue completion.
 *
 * <p>The issue is stored as a plain identifier ({@code issueId}) without a foreign key,
 * because the event must survive the issue being deleted from the application.</p>
 *
 * <p>{@code estimation} is the issue estimation at the time of the event (may be {@code null}
 * when the issue has no estimation).</p>
 */
@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "sprint_issue_event",
        indexes = @Index(name = "idx_sprint_issue_event_sprint_time", columnList = "SprintID, occurredAt")
)
public class SprintIssueEvent implements Serializable {

    @Id
    @Column(name = "SprintIssueEventID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sprint_issue_event_seq_gen")
    @SequenceGenerator(name = "sprint_issue_event_seq_gen", sequenceName = "sprint_issue_event_seq")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SprintID", nullable = false)
    private Sprint sprint;

    @Column(nullable = false)
    private Integer issueId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SprintEventType eventType;

    private Integer estimation;

    @Column(nullable = false)
    private ZonedDateTime occurredAt;
}
