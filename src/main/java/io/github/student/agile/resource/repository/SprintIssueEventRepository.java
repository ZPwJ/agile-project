package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.SprintIssueEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SprintIssueEventRepository extends JpaRepository<SprintIssueEvent, Integer> {

    /**
     * Events of a single sprint in chronological order.
     * Events with equal timestamps are ordered by id, which keeps the result deterministic.
     */
    List<SprintIssueEvent> findAllBySprintIdOrderByOccurredAtAscIdAsc(Integer sprintId);

    /**
     * Events of many sprints in a single query (e.g. for the project sprint comparison),
     * sorted by sprint and chronologically within each sprint.
     */
    List<SprintIssueEvent> findAllBySprintIdInOrderBySprintIdAscOccurredAtAscIdAsc(Collection<Integer> sprintIds);
}
