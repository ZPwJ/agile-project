package io.github.student.agile.repository;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.SprintEventType;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.ZonedDateTime;
import java.util.List;

import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.resource.domain.dictionary.SprintEventType.ADDED;
import static io.github.student.agile.resource.domain.dictionary.SprintEventType.COMPLETED;
import static io.github.student.agile.resource.domain.dictionary.SprintEventType.SCOPE_AT_START;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
class SprintIssueEventRepositoryTest {

    private static final ZonedDateTime BASE_TIME = ZonedDateTime.parse("2026-11-02T09:00:00+01:00");

    @Autowired
    private SprintIssueEventRepository eventRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @MockBean
    private UserService userService;

    private Sprint sprint;
    private Sprint otherSprint;

    @BeforeEach
    void setUp() {
        Project project = projectRepository.save(createProjectEntity());
        sprint = sprintRepository.save(createSprintEntity(project, TRUE));

        Sprint other = createSprintEntity(project, Boolean.FALSE);
        other.setName("Other sprint");
        otherSprint = sprintRepository.save(other);
    }

    @Test
    void findAllBySprintId_returnsOnlyEventsOfGivenSprintInChronologicalOrder() {
        // given: saved in non-chronological order
        SprintIssueEvent completed = eventRepository.save(event(sprint, 1, COMPLETED, 3, BASE_TIME.plusDays(2)));
        SprintIssueEvent atStart = eventRepository.save(event(sprint, 1, SCOPE_AT_START, 3, BASE_TIME));
        SprintIssueEvent added = eventRepository.save(event(sprint, 2, ADDED, null, BASE_TIME.plusDays(1)));
        eventRepository.save(event(otherSprint, 3, SCOPE_AT_START, 5, BASE_TIME));

        // when
        List<SprintIssueEvent> result = eventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprint.getId());

        // then
        assertEquals(List.of(atStart.getId(), added.getId(), completed.getId()),
                result.stream().map(SprintIssueEvent::getId).toList());
        assertNull(result.get(1).getEstimation(), "missing estimation must be stored as null");
    }

    @Test
    void findAllBySprintId_usesIdAsTieBreakerForEqualTimestamps() {
        // given
        SprintIssueEvent first = eventRepository.save(event(sprint, 1, SCOPE_AT_START, 2, BASE_TIME));
        SprintIssueEvent second = eventRepository.save(event(sprint, 2, SCOPE_AT_START, 5, BASE_TIME));

        // when
        List<SprintIssueEvent> result = eventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprint.getId());

        // then
        assertEquals(List.of(first.getId(), second.getId()),
                result.stream().map(SprintIssueEvent::getId).toList());
    }

    @Test
    void findAllBySprintIdIn_returnsEventsOfManySprintsGroupedBySprint() {
        // given
        eventRepository.save(event(otherSprint, 3, SCOPE_AT_START, 5, BASE_TIME));
        eventRepository.save(event(sprint, 1, COMPLETED, 3, BASE_TIME.plusDays(1)));
        eventRepository.save(event(sprint, 1, SCOPE_AT_START, 3, BASE_TIME));

        // when
        List<SprintIssueEvent> result = eventRepository
                .findAllBySprintIdInOrderBySprintIdAscOccurredAtAscIdAsc(List.of(sprint.getId(), otherSprint.getId()));

        // then
        assertEquals(3, result.size());
        List<Integer> sprintIds = result.stream().map(e -> e.getSprint().getId()).toList();
        assertEquals(sprintIds.stream().sorted().toList(), sprintIds);
        List<SprintIssueEvent> ofSprint = result.stream()
                .filter(e -> e.getSprint().getId().equals(sprint.getId()))
                .toList();
        assertEquals(SCOPE_AT_START, ofSprint.get(0).getEventType());
        assertEquals(COMPLETED, ofSprint.get(1).getEventType());
    }

    @Test
    void findAllBySprintId_returnsEmptyListForSprintWithoutHistory() {
        assertTrue(eventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(otherSprint.getId()).isEmpty());
    }

    private static SprintIssueEvent event(Sprint sprint, Integer issueId, SprintEventType type,
                                          Integer estimation, ZonedDateTime occurredAt) {
        return SprintIssueEvent.builder()
                .sprint(sprint)
                .issueId(issueId)
                .eventType(type)
                .estimation(estimation)
                .occurredAt(occurredAt)
                .build();
    }
}
