package io.github.student.agile.service;

import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.SprintIssueEventType;
import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO.SprintResult;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.SprintComparisonServiceImpl;
import io.github.student.agile.resource.service.analytics.SprintMetricsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;

import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.COMPLETED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.SCOPE_AT_START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests of the queries issued by the sprint comparison, without the Spring context.
 */
class SprintComparisonServiceImplTest {

    private static final Integer PROJECT_ID = 1;
    private static final ZonedDateTime STARTED_AT = ZonedDateTime.parse("2026-11-02T09:00:00+01:00");

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final SprintRepository sprintRepository = mock(SprintRepository.class);
    private final SprintIssueEventRepository eventRepository = mock(SprintIssueEventRepository.class);

    private final SprintComparisonServiceImpl service = new SprintComparisonServiceImpl(
            projectRepository, sprintRepository, eventRepository, new SprintMetricsCalculator());

    private int sequence = 0;

    @BeforeEach
    void setUp() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    }

    @Test
    void compareSprints_loadsEventsOfAllSprintsWithOneQuery() {

        //given: events of both sprints are interleaved in the query result
        Sprint first = sprintWithHistory(10, STARTED_AT);
        Sprint second = sprintWithHistory(20, STARTED_AT.plusDays(14));
        when(sprintRepository.findAllByProjectIdAndIsDoneTrue(PROJECT_ID)).thenReturn(List.of(second, first));
        when(eventRepository.findAllBySprintIDInOrderByOccurredAtAscIdAsc(List.of(10, 20))).thenReturn(List.of(
                event(10, 1, SCOPE_AT_START, 5),
                event(20, 2, SCOPE_AT_START, 8),
                event(10, 3, SCOPE_AT_START, 3),
                event(20, 2, COMPLETED, 8),
                event(10, 1, COMPLETED, 5)));

        //when
        List<SprintResult> sprints = service.compareSprints(PROJECT_ID).getSprints();

        //then: one query for all sprints, and events are not mixed between sprints
        verify(eventRepository, times(1)).findAllBySprintIDInOrderByOccurredAtAscIdAsc(any());
        verify(eventRepository, never()).findAllBySprintIDOrderByOccurredAtAscIdAsc(anyInt());
        assertEquals(new SprintResult(10, "Sprint 10", first.getStartedAt(), first.getEndedAt(), true, 1, 1, 5), sprints.get(0));
        assertEquals(new SprintResult(20, "Sprint 20", second.getStartedAt(), second.getEndedAt(), true, 1, 0, 8), sprints.get(1));
    }

    @Test
    void compareSprints_noSprintWithHistory_skipsEventsQuery() {

        //given
        Sprint legacy = Sprint.builder().id(10).name("Legacy").isDone(true).sprintIssues(new HashSet<>()).build();
        when(sprintRepository.findAllByProjectIdAndIsDoneTrue(PROJECT_ID)).thenReturn(List.of(legacy));

        //when
        List<SprintResult> sprints = service.compareSprints(PROJECT_ID).getSprints();

        //then
        verifyNoInteractions(eventRepository);
        assertEquals(new SprintResult(10, "Legacy", null, null, false, 0, 0, 0), sprints.get(0));
    }

    @Test
    void compareSprints_notExistingProject_doesNotQuerySprints() {

        //given
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

        //when
        assertThrows(ResourceNotFoundException.class, () -> service.compareSprints(PROJECT_ID));

        //then
        verifyNoInteractions(sprintRepository, eventRepository);
    }

    private static Sprint sprintWithHistory(Integer id, ZonedDateTime startedAt) {
        return Sprint.builder()
                .id(id)
                .name("Sprint " + id)
                .startedAt(startedAt)
                .endedAt(startedAt.plusDays(7))
                .active(false)
                .isDone(true)
                .sprintIssues(new HashSet<>())
                .build();
    }

    private SprintIssueEvent event(Integer sprintID, Integer issueID, SprintIssueEventType type, Integer estimation) {
        sequence++;
        return new SprintIssueEvent(sequence, sprintID, issueID, type, estimation, STARTED_AT.plusMinutes(sequence));
    }
}
