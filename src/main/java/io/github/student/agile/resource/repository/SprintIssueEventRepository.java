package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.SprintIssueEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SprintIssueEventRepository extends JpaRepository<SprintIssueEvent, Integer> {

    List<SprintIssueEvent> findAllBySprintIDOrderByOccurredAtAscIdAsc(Integer sprintID);

    /** Events of many sprints with one query; grouped by sprint they keep the chronological order. */
    List<SprintIssueEvent> findAllBySprintIDInOrderByOccurredAtAscIdAsc(Collection<Integer> sprintIDs);
}
