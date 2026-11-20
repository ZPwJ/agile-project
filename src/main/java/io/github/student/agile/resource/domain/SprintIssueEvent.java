package io.github.student.agile.resource.domain;

import io.github.student.agile.resource.domain.dictionary.SprintEventType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.ZonedDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(
        name = "sprint_issue_events",
        indexes = {
                @Index(name = "idx_sprint_issue_events_sprint_occurred", columnList = "sprint_id, occurred_at, id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
public class SprintIssueEvent implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sprint_issue_event_seq_gen")
    @SequenceGenerator(name = "sprint_issue_event_seq_gen", sequenceName = "sprint_issue_event_seq")
    private Long id;

    @Column(name = "sprint_id", nullable = false)
    private Integer sprintId;

    @Column(name = "issue_id")
    private Integer issueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private SprintEventType eventType;

    @Column(name = "previous_status_id")
    private Integer previousStatusId;

    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "previous_estimation")
    private Integer previousEstimation;

    @Column(name = "estimation")
    private Integer estimation;

    @Column(name = "occurred_at", nullable = false)
    private ZonedDateTime occurredAt;
}
