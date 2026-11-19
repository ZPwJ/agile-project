package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.NullIdentifierException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.domain.dictionary.SprintEventType;
import io.github.student.agile.resource.dto.BurnupChartDTO;
import io.github.student.agile.resource.dto.BurnupPointDTO;
import io.github.student.agile.resource.dto.SprintComparisonItemDTO;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.student.agile.exception.definition.InvalidDateRangeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintAnalyticsServiceImpl implements SprintAnalyticsService {

    public static final String DEFAULT_ESTIMATION_UNIT = "STORY_POINTS";

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final SprintIssueEventRepository sprintIssueEventRepository;
    private final SprintVelocityCalculator sprintVelocityCalculator;
    private final UserService userService;

    @Override
    public SprintSummaryReportDTO getSprintSummaryReport(Integer sprintId) {
        Sprint sprint = findSprintAndVerifyAccess(sprintId);
        Integer projectId = sprint.getProject().getId();

        List<Issue> currentIssues = issueRepository.findAllBySprintId(sprintId);
        List<SprintIssueEvent> events = sprintIssueEventRepository.findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId);

        int initialIssueCount = resolveInitialIssueCount(events, currentIssues.size());
        int completedIssueCount = (int) currentIssues.stream()
                .filter(IssueStatusID::isCompleted)
                .count();
        int velocity = sprintVelocityCalculator.calculateVelocity(sprintId, currentIssues);

        int uncompletedIssueCount;
        int uncompletedEstimationSum;

        if (Boolean.TRUE.equals(sprint.getIsDone()) && !events.isEmpty()) {
            List<IssueSnapshot> uncompletedAtStop = resolveUncompletedSnapshotsAtStop(events);
            uncompletedIssueCount = uncompletedAtStop.size();
            uncompletedEstimationSum = uncompletedAtStop.stream()
                    .mapToInt(snapshot -> sprintVelocityCalculator.safeEstimation(snapshot.estimation()))
                    .sum();
        } else {
            List<Issue> uncompletedCurrent = currentIssues.stream()
                    .filter(issue -> !IssueStatusID.isCompleted(issue))
                    .toList();
            uncompletedIssueCount = uncompletedCurrent.size();
            uncompletedEstimationSum = uncompletedCurrent.stream()
                    .mapToInt(sprintVelocityCalculator::safeEstimation)
                    .sum();
        }

        return SprintSummaryReportDTO.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .projectId(projectId)
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .initialIssueCount(initialIssueCount)
                .completedIssueCount(completedIssueCount)
                .uncompletedIssueCount(uncompletedIssueCount)
                .velocity(velocity)
                .uncompletedEstimationSum(uncompletedEstimationSum)
                .estimationUnit(DEFAULT_ESTIMATION_UNIT)
                .build();
    }

    @Override
    public BurnupChartDTO getBurnupChart(Integer sprintId) {
        Sprint sprint = findSprintAndVerifyAccess(sprintId);
        Integer projectId = sprint.getProject().getId();

        List<SprintIssueEvent> sprintEvents = sprintIssueEventRepository
                .findAllBySprintIdOrderByOccurredAtAscIdAsc(sprintId)
                .stream()
                .filter(event -> event != null && sprintId.equals(event.getSprintId()))
                .toList();

        List<BurnupPointDTO> points = buildBurnupPoints(sprint, sprintEvents);

        return BurnupChartDTO.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .projectId(projectId)
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .unit(DEFAULT_ESTIMATION_UNIT)
                .points(points)
                .build();
    }

    @Override
    public List<SprintComparisonItemDTO> getProjectSprintsComparison(Integer projectId, ZonedDateTime from, ZonedDateTime to) {
        if (projectId == null) {
            throw new NullIdentifierException();
        }
        validateDateRange(from, to);

        userService.verifyProjectAccess(projectId);

        List<Sprint> qualifyingSprints = sprintRepository.findAllByProjectIdOrderByStartDateAscIdAsc(projectId).stream()
                .filter(sprint -> sprint != null
                        && (sprint.getProject() == null || projectId.equals(sprint.getProject().getId()))
                        && (Boolean.TRUE.equals(sprint.getIsDone()) || Boolean.TRUE.equals(sprint.getActive())))
                .filter(sprint -> overlapsRange(sprint, from, to))
                .sorted(Comparator
                        .comparing(Sprint::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Sprint::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (qualifyingSprints.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> sprintIds = qualifyingSprints.stream()
                .map(Sprint::getId)
                .toList();

        Map<Integer, List<Issue>> issuesBySprintId = issueRepository.findAllBySprintIdIn(sprintIds).stream()
                .filter(issue -> issue != null && issue.getSprint() != null && issue.getSprint().getId() != null)
                .collect(Collectors.groupingBy(issue -> issue.getSprint().getId()));

        Map<Integer, List<SprintIssueEvent>> eventsBySprintId = sprintIssueEventRepository
                .findAllBySprintIdInOrderByOccurredAtAscIdAsc(sprintIds).stream()
                .filter(event -> event != null && event.getSprintId() != null)
                .collect(Collectors.groupingBy(SprintIssueEvent::getSprintId));

        return qualifyingSprints.stream()
                .map(sprint -> toComparisonItem(
                        projectId,
                        sprint,
                        issuesBySprintId.getOrDefault(sprint.getId(), Collections.emptyList()),
                        eventsBySprintId.getOrDefault(sprint.getId(), Collections.emptyList())
                ))
                .toList();
    }

    private SprintComparisonItemDTO toComparisonItem(
            Integer projectId,
            Sprint sprint,
            List<Issue> sprintIssues,
            List<SprintIssueEvent> sprintEvents
    ) {
        Integer sprintId = sprint.getId();
        int completedIssueCount = (int) sprintIssues.stream()
                .filter(IssueStatusID::isCompleted)
                .count();
        int velocity = sprintVelocityCalculator.calculateVelocity(sprintId, sprintIssues);
        int uncompletedIssueCount = (Boolean.TRUE.equals(sprint.getIsDone()) && !sprintEvents.isEmpty())
                ? resolveUncompletedSnapshotsAtStop(sprintEvents).size()
                : (int) sprintIssues.stream().filter(issue -> !IssueStatusID.isCompleted(issue)).count();

        return SprintComparisonItemDTO.builder()
                .projectId(projectId)
                .sprintId(sprintId)
                .sprintName(sprint.getName())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .completedIssueCount(completedIssueCount)
                .uncompletedIssueCount(uncompletedIssueCount)
                .velocity(velocity)
                .build();
    }

    private Sprint findSprintAndVerifyAccess(Integer sprintId) {
        if (sprintId == null) {
            throw new NullIdentifierException();
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException(SPRINT_DOES_NOT_EXISTS));

        userService.verifyProjectAccess(sprint.getProject().getId());
        return sprint;
    }
    private void validateDateRange(ZonedDateTime from, ZonedDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException();
        }
    }

    /**
     * Sprint spełnia filtr, gdy jego okres [start, end] nachodzi na [from, to] (granice włącznie).
     * - sprint aktywny (brak endDate) trwa bez końca,
     * - sprint zakończony bez endDate (dane legacy) jest traktowany jako punkt w startDate,
     * - sprint bez żadnej daty nie daje się umieścić w czasie, więc przy aktywnym filtrze jest pomijany.
     */
    private boolean overlapsRange(Sprint sprint, ZonedDateTime from, ZonedDateTime to) {
        if (from == null && to == null) {
            return true;
        }
        ZonedDateTime sprintStart = sprint.getStartDate() != null ? sprint.getStartDate() : sprint.getEndDate();
        if (sprintStart == null) {
            return false;
        }
        ZonedDateTime sprintEnd = sprint.getEndDate();
        if (sprintEnd == null && Boolean.TRUE.equals(sprint.getIsDone())) {
            sprintEnd = sprintStart;
        }

        boolean startsBeforeRangeEnd = to == null || !sprintStart.isAfter(to);
        boolean endsAfterRangeStart = from == null || sprintEnd == null || !sprintEnd.isBefore(from);
        return startsBeforeRangeEnd && endsAfterRangeStart;
    }

    private List<BurnupPointDTO> buildBurnupPoints(Sprint sprint, List<SprintIssueEvent> events) {
        if (events.isEmpty()) {
            return buildFallbackBurnupPoints(sprint);
        }

        List<SprintIssueEvent> timelineEvents = new ArrayList<>();
        LocalDate stopEventDay = null;
        for (SprintIssueEvent event : events) {
            if (event.getEventType() == SprintEventType.SPRINT_STOPPED) {
                if (event.getOccurredAt() != null) {
                    stopEventDay = toUtcDate(event.getOccurredAt());
                }
                break;
            }
            timelineEvents.add(event);
        }

        LocalDate startDay = resolveStartDay(sprint, timelineEvents, stopEventDay);
        if (startDay == null) {
            return Collections.emptyList();
        }
        LocalDate endDay = resolveEndDay(sprint, timelineEvents, stopEventDay, startDay);

        Map<Integer, IssueSnapshot> scope = new LinkedHashMap<>();
        List<BurnupPointDTO> points = new ArrayList<>();
        int eventIndex = 0;

        for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1)) {
            while (eventIndex < timelineEvents.size()) {
                SprintIssueEvent event = timelineEvents.get(eventIndex);
                LocalDate eventDay = event.getOccurredAt() != null ? toUtcDate(event.getOccurredAt()) : startDay;
                if (eventDay.isAfter(day)) {
                    break;
                }
                applyEvent(scope, event);
                eventIndex++;
            }

            int totalScope = scope.values().stream()
                    .mapToInt(snapshot -> sprintVelocityCalculator.safeEstimation(snapshot.estimation()))
                    .sum();
            int completedWork = scope.values().stream()
                    .filter(snapshot -> IssueStatusID.isCompleted(snapshot.statusId()))
                    .mapToInt(snapshot -> sprintVelocityCalculator.safeEstimation(snapshot.estimation()))
                    .sum();

            points.add(BurnupPointDTO.builder()
                    .date(day)
                    .completedWork(Math.max(0, completedWork))
                    .totalScope(Math.max(0, totalScope))
                    .unit(DEFAULT_ESTIMATION_UNIT)
                    .build());
        }

        return points;
    }

    private List<BurnupPointDTO> buildFallbackBurnupPoints(Sprint sprint) {
        if (sprint.getStartDate() == null) {
            return Collections.emptyList();
        }

        Integer sprintId = sprint.getId();
        List<Issue> sprintIssues = issueRepository.findAllBySprintId(sprintId).stream()
                .filter(issue -> issue != null
                        && issue.getSprint() != null
                        && sprintId.equals(issue.getSprint().getId()))
                .toList();

        int totalScope = sprintIssues.stream()
                .mapToInt(sprintVelocityCalculator::safeEstimation)
                .sum();
        int completedWork = sprintVelocityCalculator.calculateVelocity(sprintId, sprintIssues);

        ZonedDateTime pointTimestamp = sprint.getEndDate() != null ? sprint.getEndDate() : sprint.getStartDate();
        return List.of(BurnupPointDTO.builder()
                .date(toUtcDate(pointTimestamp))
                .completedWork(Math.max(0, completedWork))
                .totalScope(Math.max(0, totalScope))
                .unit(DEFAULT_ESTIMATION_UNIT)
                .build());
    }

    private LocalDate resolveStartDay(Sprint sprint, List<SprintIssueEvent> timelineEvents, LocalDate stopEventDay) {
        if (sprint.getStartDate() != null) {
            return toUtcDate(sprint.getStartDate());
        }
        return timelineEvents.stream()
                .map(SprintIssueEvent::getOccurredAt)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .map(this::toUtcDate)
                .orElse(stopEventDay);
    }

    private LocalDate resolveEndDay(
            Sprint sprint,
            List<SprintIssueEvent> timelineEvents,
            LocalDate stopEventDay,
            LocalDate startDay
    ) {
        LocalDate endDay;
        if (sprint.getEndDate() != null) {
            endDay = toUtcDate(sprint.getEndDate());
        } else if (stopEventDay != null) {
            endDay = stopEventDay;
        } else {
            endDay = LocalDate.now(ZoneOffset.UTC);
        }

        if (endDay.isBefore(startDay)) {
            endDay = startDay;
        }
        if (sprint.getEndDate() == null) {
            for (SprintIssueEvent event : timelineEvents) {
                if (event.getOccurredAt() != null) {
                    LocalDate eventDay = toUtcDate(event.getOccurredAt());
                    if (eventDay.isAfter(endDay)) {
                        endDay = eventDay;
                    }
                }
            }
        }
        return endDay;
    }

    private LocalDate toUtcDate(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    private int resolveInitialIssueCount(List<SprintIssueEvent> events, int fallbackCount) {
        int startedCount = (int) events.stream()
                .filter(event -> event.getEventType() == SprintEventType.SPRINT_STARTED)
                .count();
        return startedCount > 0 ? startedCount : fallbackCount;
    }

    private List<IssueSnapshot> resolveUncompletedSnapshotsAtStop(List<SprintIssueEvent> events) {
        Map<Integer, IssueSnapshot> scopeAtStop = new LinkedHashMap<>();

        for (SprintIssueEvent event : events) {
            if (event.getEventType() == SprintEventType.SPRINT_STOPPED) {
                break;
            }
            applyEvent(scopeAtStop, event);
        }

        return scopeAtStop.values().stream()
                .filter(snapshot -> !IssueStatusID.isCompleted(snapshot.statusId()))
                .toList();
    }

    private void applyEvent(Map<Integer, IssueSnapshot> scope, SprintIssueEvent event) {
        Integer issueId = event.getIssueId();
        if (issueId == null) {
            return;
        }
        switch (event.getEventType()) {
            case SPRINT_STARTED, ADDED_TO_SPRINT, STATUS_CHANGED ->
                    scope.put(issueId, new IssueSnapshot(event.getStatusId(), event.getEstimation()));
            case REMOVED_FROM_SPRINT -> scope.remove(issueId);
            case ESTIMATION_CHANGED -> scope.compute(issueId, (id, existing) -> new IssueSnapshot(
                    event.getStatusId() != null ? event.getStatusId() : (existing != null ? existing.statusId() : null),
                    event.getEstimation()
            ));
            case SPRINT_STOPPED -> {
            }
        }
    }

    private record IssueSnapshot(Integer statusId, Integer estimation) {
    }
}
