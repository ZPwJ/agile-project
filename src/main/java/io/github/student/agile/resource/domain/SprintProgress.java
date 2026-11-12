package io.github.student.agile.resource.domain;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.ZonedDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = @Index(columnList = "sprint_id,captured_at,id"))
public class SprintProgress {

    public enum Kind { START, CHANGE, END }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sprint_progress_seq_gen")
    @SequenceGenerator(name = "sprint_progress_seq_gen", sequenceName = "sprint_progress_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sprint_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Sprint sprint;

    @Column(name = "captured_at", nullable = false)
    private ZonedDateTime capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    private int issueCount;
    private int completedIssueCount;
    private long totalPoints;
    private long completedPoints;
}
