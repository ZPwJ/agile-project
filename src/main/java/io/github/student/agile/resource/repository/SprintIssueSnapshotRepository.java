package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.SprintIssueSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SprintIssueSnapshotRepository extends JpaRepository<SprintIssueSnapshot, Integer> {

    List<SprintIssueSnapshot> findAllBySprintId(Integer sprintId);

    /** Batch variant so comparison reports need one query, not one per sprint. */
    List<SprintIssueSnapshot> findAllBySprintIdIn(Collection<Integer> sprintIds);
}
