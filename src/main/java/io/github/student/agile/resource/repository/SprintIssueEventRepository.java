package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.SprintEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintIssueEventRepository extends JpaRepository<SprintIssueEvent, Long> {

    List<SprintIssueEvent> findAllBySprintIdOrderByOccurredAtAscIdAsc(Integer sprintId);

    List<SprintIssueEvent> findAllBySprintIdAndEventTypeOrderByOccurredAtAscIdAsc(Integer sprintId, SprintEventType eventType);

    void deleteAllBySprintId(Integer sprintId);
}
