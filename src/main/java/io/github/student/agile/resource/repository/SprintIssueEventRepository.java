package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.SprintIssueEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SprintIssueEventRepository extends JpaRepository<SprintIssueEvent, Integer> {

    List<SprintIssueEvent> findAllBySprintIDInOrderByOccurredAtAscIdAsc(Collection<Integer> sprintIDs);

    Optional<SprintIssueEvent> findFirstBySprintIDAndIssueIDAndTypeInOrderByOccurredAtDescIdDesc(
            Integer sprintID, Integer issueID, Collection<SprintIssueEventType> types);
}
