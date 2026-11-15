package io.github.student.agile.resource.service.analytics;

import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.service.analytics.SprintIssueStates.IssueState;
import io.github.student.agile.resource.service.analytics.SprintProgress.DailyProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.SCOPE_AT_START;
import static io.github.student.agile.resource.service.analytics.SprintIssueStates.sumOf;
import static java.util.Objects.isNull;

/**
 * Rebuilds the day-by-day progress of a sprint from its recorded events (WF-3, WF-4).
 * <p>
 * Day 0 is the moment the sprint started: only the {@code SCOPE_AT_START} events are applied, so nothing is
 * completed yet and the scope equals the initial scope. Day {@code n} (n >= 1) is the state at the end of the
 * n-th calendar day of the sprint, in {@link #REPORT_ZONE}: all events that occurred on that day or earlier are
 * replayed with {@link SprintIssueStates}, the same rules the sprint report and velocity use, so the completed work
 * of the last day equals the sprint velocity.
 * <p>
 * The last day is the day the sprint ended, or today for a sprint that is still running. A sprint without history
 * (not started, or started before events were recorded) yields no days: its progress cannot be reconstructed,
 * and the charts must not present made-up data.
 */
@Component
@RequiredArgsConstructor
public class SprintProgressCalculator {

    public static final ZoneId REPORT_ZONE = ZoneId.of("Europe/Warsaw");

    private final SprintMetricsCalculator sprintMetricsCalculator;

    /**
     * @param chronologicalEvents events of this sprint only, ordered by occurrence
     * @param now                 current time, used as the last day of a sprint that has not ended yet
     */
    public SprintProgress calculate(Sprint sprint, List<SprintIssueEvent> chronologicalEvents, ZonedDateTime now) {

        if (!sprintMetricsCalculator.hasHistory(sprint)) {
            return SprintProgress.withoutHistory();
        }

        LocalDate firstDay = toReportDate(sprint.getStartedAt());
        LocalDate lastDay = toReportDate(isNull(sprint.getEndedAt()) ? now : sprint.getEndedAt());
        if (lastDay.isBefore(firstDay)) {
            lastDay = firstDay;
        }

        List<SprintIssueEvent> scopeAtStart = chronologicalEvents.stream()
                .filter(event -> event.getType() == SCOPE_AT_START)
                .toList();
        SprintIssueStates startState = SprintIssueStates.replay(scopeAtStart);
        int initialScope = totalScope(startState);

        List<DailyProgress> days = new ArrayList<>();
        days.add(snapshot(0, firstDay, startState));

        // Events are chronological, so the events up to a given day always form a prefix of the list.
        int eventsUpToDay = 0;
        int dayNumber = 1;
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1), dayNumber++) {
            while (eventsUpToDay < chronologicalEvents.size()
                    && !toReportDate(chronologicalEvents.get(eventsUpToDay).getOccurredAt()).isAfter(date)) {
                eventsUpToDay++;
            }
            SprintIssueStates state = SprintIssueStates.replay(chronologicalEvents.subList(0, eventsUpToDay));
            days.add(snapshot(dayNumber, date, state));
        }
        return new SprintProgress(true, initialScope, days);
    }

    private DailyProgress snapshot(int day, LocalDate date, SprintIssueStates state) {
        return new DailyProgress(day, date, sprintMetricsCalculator.velocity(state), totalScope(state));
    }

    /** Completed issues count with their estimation at completion, so the scope never drops below completed work. */
    private int totalScope(SprintIssueStates state) {
        return sprintMetricsCalculator.velocity(state)
                + sumOf(state.uncompletedIssues().stream().map(IssueState::getEstimation).toList());
    }

    private static LocalDate toReportDate(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(REPORT_ZONE).toLocalDate();
    }
}
