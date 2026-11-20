package io.github.student.agile.resource.analytics.charts;

import io.github.student.agile.resource.domain.IssueStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SprintChartRepository extends JpaRepository<IssueStatusHistory, Integer> {

    /** One flat query per sprint: every change, grouped per task in Java, no entity graph. */
    @Query("""
            select h.issueId as issueId,
                   h.newStatus.id as newStatusId,
                   h.changedAt as changedAt
            from IssueStatusHistory h
            where h.sprintId = :sprintId
            order by h.issueId asc, h.changedAt asc, h.id asc
            """)
    List<SprintStatusChange> findStatusChangesBySprintId(@Param("sprintId") Integer sprintId);

    /** Moment the first change was ever recorded: history does not exist before it. */
    @Query("select min(h.changedAt) from IssueStatusHistory h")
    Optional<Instant> findHistoryRecordingStart();

    interface SprintStatusChange {

        Integer getIssueId();

        Integer getNewStatusId();

        Instant getChangedAt();
    }
}
