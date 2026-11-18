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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Result of one issue in a finished sprint, written when the sprint is stopped.
 * Reports read these rows instead of live issues, because stopping a sprint detaches its issues
 * (sprint = null) and resets unfinished ones to TO_DO.
 * <p>
 * Limitation: the snapshot reflects scope and estimation at the moment the sprint was stopped.
 * Issues removed from the sprint earlier, and estimation changes during the sprint, are not recorded.
 */
@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sprint_issue_snapshot", indexes = {
        @Index(name = "idx_sis_sprint", columnList = "SprintID")
})
public class SprintIssueSnapshot implements Serializable {

    @Id
    @Column(name = "SnapshotID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sprint_issue_snapshot_seq_gen")
    @SequenceGenerator(name = "sprint_issue_snapshot_seq_gen", sequenceName = "sprint_issue_snapshot_seq")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SprintID", nullable = false)
    private Sprint sprint;

    /** Plain id (no FK) so the row survives deletion of the issue. */
    @Column(name = "issue_id", nullable = false)
    private Integer issueId;

    /** Story points at stop time; {@code null} = not estimated. */
    private Integer estimation;

    /** Status at stop time, before unfinished issues are reset to TO_DO. */
    @ManyToOne
    @JoinColumn(name = "finalStatusId")
    private IssueStatus finalStatus;

    /** True when the final status was DONE. */
    @Column(nullable = false)
    private Boolean completed;
}
