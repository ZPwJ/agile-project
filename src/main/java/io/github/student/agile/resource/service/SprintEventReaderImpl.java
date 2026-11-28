package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SprintEventReaderImpl implements SprintEventReader {

    private final SprintIssueEventRepository sprintIssueEventRepository;

    @Override
    public List<SprintIssueEvent> findBySprintIDs(Collection<Integer> sprintIDs) {

        if (sprintIDs.isEmpty()) {
            return List.of();
        }
        return sprintIssueEventRepository.findAllBySprintIDInOrderByOccurredAtAscIdAsc(sprintIDs);
    }
}
