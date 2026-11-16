package io.github.student.agile.resource.service.analytics;

import io.github.student.agile.exception.definition.InvalidRequestException;
import io.github.student.agile.resource.domain.Sprint;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.INVALID_DATE_RANGE;
import static io.github.student.agile.resource.service.analytics.SprintProgressCalculator.REPORT_ZONE;
import static java.util.Objects.isNull;

/**
 * Date filter of sprint reports (WF-6). Both bounds are optional and inclusive calendar days in {@link SprintProgressCalculator#REPORT_ZONE}.
 * <p>
 * A sprint matches when its period {@code startedAt}–{@code endedAt} overlaps the range: it ended on or after {@code from}
 * and started on or before {@code to}. A bound that needs a date the sprint does not have (a sprint finished before
 * the analytics module recorded history) is not met, because the overlap cannot be verified.
 *
 * @param from first day of the range, {@code null} for no lower bound
 * @param to   last day of the range, {@code null} for no upper bound
 * @throws InvalidRequestException when {@code from} is later than {@code to}
 */
public record SprintDateRange(LocalDate from, LocalDate to) {

    public SprintDateRange {
        if (!isNull(from) && !isNull(to) && from.isAfter(to)) {
            throw new InvalidRequestException(INVALID_DATE_RANGE);
        }
    }

    public boolean includes(Sprint sprint) {
        return (isNull(from) || endedOnOrAfter(sprint.getEndedAt(), from))
                && (isNull(to) || startedOnOrBefore(sprint.getStartedAt(), to));
    }

    private static boolean endedOnOrAfter(ZonedDateTime endedAt, LocalDate day) {
        return !isNull(endedAt) && !dayOf(endedAt).isBefore(day);
    }

    private static boolean startedOnOrBefore(ZonedDateTime startedAt, LocalDate day) {
        return !isNull(startedAt) && !dayOf(startedAt).isAfter(day);
    }

    private static LocalDate dayOf(ZonedDateTime moment) {
        return moment.withZoneSameInstant(REPORT_ZONE).toLocalDate();
    }
}
