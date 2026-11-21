package io.github.student.agile.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * One row per issue status change. Source of truth for burndown/burnup (when work was completed).
 * History starts at deployment: issues changed earlier have no rows and are never back-filled.
 * <p>
 * {@code sprint} is stored as a plain id (not a foreign key) so rows survive deletion of the sprint,
 * which the current "stop sprint" flow performs.
 */
@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "issue_status_history", indexes = {
        @Index(name = "idx_ish_issue_changed", columnList = "issue_id, changedAt"),
        @Index(name = "idx_ish_sprint_status_changed", columnList = "sprintId, newStatusId, changedAt")
})
public class IssueStatusHistory implements Serializable {

    @Id
    @Column(name = "HistoryID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "issue_status_history_seq_gen")
    @SequenceGenerator(name = "issue_status_history_seq_gen", sequenceName = "issue_status_history_seq")
    private Integer id;

    @NotNull
    @Column(name = "issue_id", nullable = false)
    private Integer issueId;

    /** Sprint the issue belonged to when the change happened; may be null. */
    @Column(name = "sprintId")
    private Integer sprintId;

    @ManyToOne
    @JoinColumn(name = "oldStatusId")
    private IssueStatus oldStatus;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "newStatusId", nullable = false)
    private IssueStatus newStatus;

    /** UTC instant; convert to a calendar day only when building charts. */
    @NotNull
    @Column(nullable = false)
    private Instant changedAt;
}
