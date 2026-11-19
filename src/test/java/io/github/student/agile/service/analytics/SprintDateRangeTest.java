package io.github.student.agile.service.analytics;

import io.github.student.agile.exception.definition.InvalidRequestException;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.service.analytics.SprintDateRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.INVALID_DATE_RANGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintDateRangeTest {

    /** Sprint lasting from 2 to 15 November 2026 in Europe/Warsaw. */
    private static final Sprint SPRINT = sprint(
            ZonedDateTime.parse("2026-11-02T09:00:00+01:00"),
            ZonedDateTime.parse("2026-11-15T17:00:00+01:00"));

    @ParameterizedTest(name = "[{index}] from={0} to={1} -> {2}")
    @CsvSource({
            // both bounds: the sprint must overlap the range, bounds are inclusive
            "2026-10-01, 2026-11-01, false",
            "2026-10-01, 2026-11-02, true",
            "2026-11-05, 2026-11-06, true",
            "2026-10-01, 2026-12-31, true",
            "2026-11-15, 2026-11-30, true",
            "2026-11-16, 2026-11-30, false",
            "2026-11-02, 2026-11-02, true",
            // only from: the sprint must end on or after it
            "2026-11-15,           , true",
            "2026-11-16,           , false",
            // only to: the sprint must start on or before it
            "          , 2026-11-02, true",
            "          , 2026-11-01, false",
            // no bounds
            "          ,           , true"
    })
    void includes_sprintOverlappingRange(LocalDate from, LocalDate to, boolean expected) {

        assertEquals(expected, new SprintDateRange(from, to).includes(SPRINT));
    }

    @Test
    void constructor_fromLaterThanTo_throwsInvalidRequestException() {

        LocalDate from = LocalDate.parse("2026-11-16");
        LocalDate to = LocalDate.parse("2026-11-15");

        Exception exception = assertThrows(InvalidRequestException.class, () -> new SprintDateRange(from, to));

        assertEquals(INVALID_DATE_RANGE, exception.getMessage());
    }

    @Test
    void constructor_sameDayOrOpenBounds_isValid() {

        LocalDate day = LocalDate.parse("2026-11-15");

        assertDoesNotThrow(() -> new SprintDateRange(day, day));
        assertDoesNotThrow(() -> new SprintDateRange(day, null));
        assertDoesNotThrow(() -> new SprintDateRange(null, day));
        assertDoesNotThrow(() -> new SprintDateRange(null, null));
    }

    @Test
    void includes_daysAreCountedInWarsawTimeZone() {

        // 23:30 UTC is already the next day in Warsaw (UTC+1 in November)
        Sprint sprint = sprint(
                ZonedDateTime.parse("2026-11-01T23:30:00Z"),
                ZonedDateTime.parse("2026-11-09T23:30:00Z"));

        assertFalse(new SprintDateRange(null, LocalDate.parse("2026-11-01")).includes(sprint));
        assertTrue(new SprintDateRange(null, LocalDate.parse("2026-11-02")).includes(sprint));
        assertTrue(new SprintDateRange(LocalDate.parse("2026-11-10"), null).includes(sprint));
        assertFalse(new SprintDateRange(LocalDate.parse("2026-11-11"), null).includes(sprint));
    }

    @Test
    void includes_sprintWithoutDates_matchesOnlyWithoutBounds() {

        // sprint finished before the analytics module recorded history
        Sprint sprint = sprint(null, null);

        assertTrue(new SprintDateRange(null, null).includes(sprint));
        assertFalse(new SprintDateRange(LocalDate.parse("2026-01-01"), null).includes(sprint));
        assertFalse(new SprintDateRange(null, LocalDate.parse("2026-12-31")).includes(sprint));
    }

    @Test
    void includes_sprintWithoutStart_isMatchedOnlyByItsEnd() {

        // sprint started before the analytics module was deployed and finished after it
        Sprint sprint = sprint(null, ZonedDateTime.parse("2026-11-15T17:00:00+01:00"));

        assertTrue(new SprintDateRange(LocalDate.parse("2026-11-15"), null).includes(sprint));
        assertFalse(new SprintDateRange(LocalDate.parse("2026-11-16"), null).includes(sprint));
        assertFalse(new SprintDateRange(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30")).includes(sprint));
    }

    private static Sprint sprint(ZonedDateTime startedAt, ZonedDateTime endedAt) {
        return Sprint.builder().id(1).startedAt(startedAt).endedAt(endedAt).isDone(true).build();
    }
}
