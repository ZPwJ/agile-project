package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.SprintIssueEvent;

import java.util.Collection;
import java.util.List;

public interface SprintEventReader {

    /**
     * @return events of the given sprints, read with one query, ordered by time and then by identifier
     */
    List<SprintIssueEvent> findBySprintIDs(Collection<Integer> sprintIDs);
}
