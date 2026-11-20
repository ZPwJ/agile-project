package io.github.student.agile.resource.analytics.charts;

import io.github.student.agile.resource.analytics.error.SprintNotFoundException;
import io.github.student.agile.resource.analytics.metrics.SprintCompletionPolicy;
import io.github.student.agile.resource.analytics.metrics.SprintMetrics;
import io.github.student.agile.resource.analytics.metrics.SprintMetricsProvider;
import io.github.student.agile.resource.analytics.metrics.SprintMetricsService;
import io.github.student.agile.resource.analytics.metrics.SprintState;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the day axis of one sprint and the points finished by the end of every day.
 * <p>
 * Sources: scope and unit from {@link SprintMetricsProvider}; tasks from {@link SprintScopeLoader};
 * "when" from one flat history query. Nothing is invented: no synthetic points, no interpolation,
 * no "assume it was done earlier". See plan sections 3.2-3.6 for the canonical rules.
 * <p>
 * Every edge rule of plan section 3.4 is marked with a {@code RULE Ex} comment next to the code
 * that implements it. The ids are part of the documentation, not of the API.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintTimelineService {

    static final ZoneId CHART_ZONE = ZoneId.of("Europe/Warsaw");

    private final SprintRepository sprintRepository;
    private final SprintMetricsProvider metricsProvider;
    private final SprintChartRepository chartRepository;
    private final SprintScopeLoader scopeLoader;
    private final IssueStatusRepository issueStatusRepository;
    private final SprintCompletionPolicy completionPolicy;

    public SprintTimeline forSprint(Integer sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));
        SprintMetrics metrics = metricsProvider.forSprint(sprintId);
        SprintState state = SprintMetricsService.stateOf(sprint);
        long scope = metrics.completedEstimate() + metrics.incompleteEstimate();
        List<SprintTaskPoints> tasks = scopeLoader.load(sprint, state);
        SprintChartWarnings warnings = new SprintChartWarnings();

        // RULE E1: without a start date nothing can be placed on a timeline.
        LocalDate start = dayOf(sprint.getStartDate());
        if (start == null) {
            warnings.add(SprintChartWarning.NO_DATES);
            return build(sprintId, metrics, scope, List.of(), warnings, true);
        }
        // RULE E2: an end date before the start date is invalid data.
        LocalDate end = endDay(sprint, state, start);
        if (end.isBefore(start)) {
            warnings.add(SprintChartWarning.INVALID_DATE_RANGE);
            return build(sprintId, metrics, scope, List.of(), warnings, true);
        }

        addActiveSprintWarning(state, warnings);
        addPlannedSprintWarning(state, warnings);
        addEmptySprintWarning(tasks, warnings);
        addUnestimatedTasksWarning(tasks, warnings);

        Map<Integer, Long> estimation = estimationOf(tasks);
        ChangeSummary changes = loadChanges(sprintId, start, end, estimation);

        addNoRecordedChangesWarning(changes.firstChangeDay(), warnings);
        addScopeChangedWarning(changes.outOfScope(), warnings);
        long undated = addUndatedCompletionWarning(tasks, changes.lastRecordedState(), warnings);
        boolean historyCovered = checkHistoryCoverage(start, warnings);

        List<SprintTimelineDay> days = buildDays(start, end, changes, estimation, historyCovered);
        addScopeNotHistoricalWarning(days, warnings);

        boolean partial = !historyCovered || undated > 0 || days.isEmpty();
        return build(sprintId, metrics, scope, days, warnings, partial);
    }

    private static void addActiveSprintWarning(SprintState state, SprintChartWarnings warnings) {
        // RULE E4: an active sprint ends today, future days are never invented.
        if (state == SprintState.ACTIVE) {
            warnings.add(SprintChartWarning.SPRINT_IN_PROGRESS);
        }
    }

    private static void addPlannedSprintWarning(SprintState state, SprintChartWarnings warnings) {
        // RULE E5: a planned sprint is shown for its planned days only.
        if (state == SprintState.PLANNED) {
            warnings.add(SprintChartWarning.SPRINT_NOT_STARTED);
        }
    }

    private static void addEmptySprintWarning(List<SprintTaskPoints> tasks,
                                              SprintChartWarnings warnings) {
        // RULE E10: empty sprint.
        if (tasks.isEmpty()) {
            warnings.add(SprintChartWarning.EMPTY_SPRINT);
        }
    }

    private static void addUnestimatedTasksWarning(List<SprintTaskPoints> tasks,
                                                   SprintChartWarnings warnings) {
        // RULE E9: a task without estimation adds 0 points.
        long unestimated = tasks.stream().filter(task -> task.estimation() == null).count();
        if (unestimated > 0) {
            warnings.add(SprintChartWarning.UNESTIMATED_TASKS, unestimated);
        }
    }

    private static Map<Integer, Long> estimationOf(List<SprintTaskPoints> tasks) {
        Map<Integer, Long> estimation = new HashMap<>();
        for (SprintTaskPoints task : tasks) {
            estimation.put(task.issueId(), task.estimation() == null ? 0L : task.estimation());
        }
        return estimation;
    }

    /** One flat history query; the rows are grouped into Warsaw days in Java. */
    private ChangeSummary loadChanges(Integer sprintId, LocalDate start, LocalDate end,
                                      Map<Integer, Long> estimation) {
        List<SprintChartRepository.SprintStatusChange> changes =
                chartRepository.findStatusChangesBySprintId(sprintId);
        Map<Integer, IssueStatus> statuses = issueStatusRepository.findAll().stream()
                .collect(Collectors.toMap(IssueStatus::getId, Function.identity()));

        Map<LocalDate, Map<Integer, Boolean>> lastStateOfDay = new HashMap<>();
        Map<Integer, Boolean> lastRecordedState = new HashMap<>();
        Set<Integer> outOfScope = new LinkedHashSet<>();
        LocalDate firstChangeDay = null;

        for (SprintChartRepository.SprintStatusChange change : changes) {
            LocalDate day = dayOf(change.getChangedAt());
            // RULE R8: changes outside the sprint dates cannot be placed on this axis.
            if (day.isBefore(start) || day.isAfter(end)) {
                continue;
            }
            boolean completed = completionPolicy.isCompleted(statuses.get(change.getNewStatusId()));
            // RULE E11: the last change of a day wins, so a reopen cancels the completion.
            lastStateOfDay.computeIfAbsent(day, key -> new LinkedHashMap<>())
                    .put(change.getIssueId(), completed);
            lastRecordedState.put(change.getIssueId(), completed);
            // RULE E7: changes of tasks that are not in the current scope are not priced.
            if (!estimation.containsKey(change.getIssueId())) {
                outOfScope.add(change.getIssueId());
            }
            if (firstChangeDay == null || day.isBefore(firstChangeDay)) {
                firstChangeDay = day;
            }
        }
        return new ChangeSummary(lastStateOfDay, lastRecordedState, outOfScope, firstChangeDay);
    }

    private static void addNoRecordedChangesWarning(LocalDate firstChangeDay,
                                                    SprintChartWarnings warnings) {
        // RULE E3: no change inside the sprint dates means no day can be backed by evidence.
        if (firstChangeDay == null) {
            warnings.add(SprintChartWarning.NO_RECORDED_CHANGES);
        }
    }

    private static void addScopeChangedWarning(Set<Integer> outOfScope,
                                               SprintChartWarnings warnings) {
        // One warning per distinct out-of-scope task; the changes themselves are dropped above (E7).
        if (!outOfScope.isEmpty()) {
            warnings.add(SprintChartWarning.SCOPE_CHANGED, outOfScope.size());
        }
    }

    private static long addUndatedCompletionWarning(List<SprintTaskPoints> tasks,
                                                    Map<Integer, Boolean> lastRecordedState,
                                                    SprintChartWarnings warnings) {
        // RULE E8: completed in the scope source but no recorded DONE change inside the axis.
        long undated = tasks.stream()
                .filter(SprintTaskPoints::completed)
                .filter(task -> !Boolean.TRUE.equals(lastRecordedState.get(task.issueId())))
                .count();
        if (undated > 0) {
            warnings.add(SprintChartWarning.UNDATED_COMPLETION, undated);
        }
        return undated;
    }

    private boolean checkHistoryCoverage(LocalDate start, SprintChartWarnings warnings) {
        // RULE E6: points may only start where the recording covers the sprint.
        boolean covered = chartRepository.findHistoryRecordingStart()
                .map(SprintTimelineService::dayOf)
                .map(recordedFrom -> !recordedFrom.isAfter(start))
                .orElse(false);
        if (!covered) {
            warnings.add(SprintChartWarning.HISTORY_STARTED_LATE);
        }
        return covered;
    }

    private static List<SprintTimelineDay> buildDays(LocalDate start, LocalDate end,
                                                     ChangeSummary changes,
                                                     Map<Integer, Long> estimation,
                                                     boolean historyCovered) {
        List<SprintTimelineDay> days = new ArrayList<>();
        if (!historyCovered && changes.firstChangeDay() == null) {
            return days;
        }
        LocalDate first = historyCovered ? start : changes.firstChangeDay();
        Set<Integer> completedTasks = new HashSet<>();
        long completedSoFar = 0;
        for (LocalDate day = first; !day.isAfter(end); day = day.plusDays(1)) {
            // Apply the changes of this day at its end; the carried state survives (E11 above).
            for (Map.Entry<Integer, Boolean> stateOfDay
                    : changes.lastStateOfDay().getOrDefault(day, Map.of()).entrySet()) {
                if (stateOfDay.getValue()) {
                    completedTasks.add(stateOfDay.getKey());
                } else {
                    completedTasks.remove(stateOfDay.getKey());
                }
            }
            long now = completedTasks.stream()
                    .mapToLong(id -> estimation.getOrDefault(id, 0L))
                    .sum();
            days.add(new SprintTimelineDay(day, now - completedSoFar, now));
            completedSoFar = now;
        }
        return days;
    }

    private static void addScopeNotHistoricalWarning(List<SprintTimelineDay> days,
                                                     SprintChartWarnings warnings) {
        // RULE E12: the scope is the value when the numbers were taken, not a historical one.
        if (!days.isEmpty()) {
            warnings.add(SprintChartWarning.SCOPE_NOT_HISTORICAL);
        }
    }

    private static SprintTimeline build(Integer sprintId, SprintMetrics metrics, long scope,
                                        List<SprintTimelineDay> days,
                                        SprintChartWarnings warnings, boolean partial) {
        return new SprintTimeline(sprintId, metrics.unit(), scope, days,
                partial ? SprintChartDataCompleteness.PARTIAL : SprintChartDataCompleteness.COMPLETE,
                warnings.toList());
    }

    private static LocalDate endDay(Sprint sprint, SprintState state, LocalDate start) {
        LocalDate today = LocalDate.now(CHART_ZONE);
        // Active sprints end at min(planned end, today), never in the future (E4).
        if (state == SprintState.ACTIVE) {
            LocalDate planned = sprint.getEndDate() == null ? today : dayOf(sprint.getEndDate());
            LocalDate end = planned.isBefore(today) ? planned : today;
            return end.isBefore(start) ? start : end;
        }
        // Planned sprints without an end date are a single day; with one they use it (E5).
        if (sprint.getEndDate() != null) {
            return dayOf(sprint.getEndDate());
        }
        // A finished sprint without an end date has no recorded end: fall back to today.
        if (state == SprintState.FINISHED) {
            return today.isBefore(start) ? start : today;
        }
        return start;
    }

    private static LocalDate dayOf(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.withZoneSameInstant(CHART_ZONE).toLocalDate();
    }

    private static LocalDate dayOf(Instant instant) {
        return instant == null ? null : LocalDate.ofInstant(instant, CHART_ZONE);
    }

    private record ChangeSummary(Map<LocalDate, Map<Integer, Boolean>> lastStateOfDay,
                                 Map<Integer, Boolean> lastRecordedState,
                                 Set<Integer> outOfScope,
                                 LocalDate firstChangeDay) {
    }
}
