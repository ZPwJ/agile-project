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
        // RULE E4: an active sprint ends today, future days are never invented.
        if (state == SprintState.ACTIVE) {
            warnings.add(SprintChartWarning.SPRINT_IN_PROGRESS);
        }
        // RULE E5: a planned sprint is shown for its planned days only.
        if (state == SprintState.PLANNED) {
            warnings.add(SprintChartWarning.SPRINT_NOT_STARTED);
        }
        // RULE E10: empty sprint.
        if (tasks.isEmpty()) {
            warnings.add(SprintChartWarning.EMPTY_SPRINT);
        }
        // RULE E9: a task without estimation adds 0 points.
        long unestimated = tasks.stream().filter(task -> task.estimation() == null).count();
        if (unestimated > 0) {
            warnings.add(SprintChartWarning.UNESTIMATED_TASKS, unestimated);
        }

        Map<Integer, Long> estimation = new HashMap<>();
        for (SprintTaskPoints task : tasks) {
            estimation.put(task.issueId(), task.estimation() == null ? 0L : task.estimation());
        }

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

        // RULE E3: no change inside the sprint dates means no day can be backed by evidence.
        if (firstChangeDay == null) {
            warnings.add(SprintChartWarning.NO_RECORDED_CHANGES);
        }
        if (!outOfScope.isEmpty()) {
            warnings.add(SprintChartWarning.SCOPE_CHANGED, outOfScope.size());
        }

        // RULE E8: completed in the scope source but no recorded DONE change inside the axis.
        long undated = tasks.stream()
                .filter(SprintTaskPoints::completed)
                .filter(task -> !Boolean.TRUE.equals(lastRecordedState.get(task.issueId())))
                .count();
        if (undated > 0) {
            warnings.add(SprintChartWarning.UNDATED_COMPLETION, undated);
        }

        // RULE E6: points may only start where the recording covers the sprint.
        boolean historyCovered = chartRepository.findHistoryRecordingStart()
                .map(SprintTimelineService::dayOf)
                .map(recordedFrom -> !recordedFrom.isAfter(start))
                .orElse(false);
        if (!historyCovered) {
            warnings.add(SprintChartWarning.HISTORY_STARTED_LATE);
        }

        List<SprintTimelineDay> days = new ArrayList<>();
        if (historyCovered || firstChangeDay != null) {
            LocalDate first = historyCovered ? start : firstChangeDay;
            Set<Integer> completedTasks = new HashSet<>();
            long completedSoFar = 0;
            for (LocalDate day = first; !day.isAfter(end); day = day.plusDays(1)) {
                for (Map.Entry<Integer, Boolean> stateOfDay
                        : lastStateOfDay.getOrDefault(day, Map.of()).entrySet()) {
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
        }
        // RULE E12: the scope is the value when the numbers were taken, not a historical one.
        if (!days.isEmpty()) {
            warnings.add(SprintChartWarning.SCOPE_NOT_HISTORICAL);
        }

        boolean partial = !historyCovered || undated > 0 || days.isEmpty();
        return build(sprintId, metrics, scope, days, warnings, partial);
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
        // RULE E4: an active sprint ends at min(planned end, today), never in the future.
        if (state == SprintState.ACTIVE) {
            LocalDate planned = sprint.getEndDate() == null ? today : dayOf(sprint.getEndDate());
            LocalDate end = planned.isBefore(today) ? planned : today;
            return end.isBefore(start) ? start : end;
        }
        // RULE E5: a planned sprint without an end date is a single day.
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
}
