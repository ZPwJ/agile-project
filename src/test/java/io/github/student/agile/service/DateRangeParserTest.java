package io.github.student.agile.service;

import io.github.student.agile.exception.definition.InvalidDateRangeException;
import io.github.student.agile.utils.DateRangeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateRangeParserTest {

    @Test
    public void parseFrom_dateOnlyStartsAtBeginningOfDayUtc() {
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), DateRangeParser.parseFrom("2026-11-01").toInstant());
    }

    @Test
    public void parseTo_dateOnlyEndsAtEndOfDayUtc() {
        assertEquals(Instant.parse("2026-11-05T23:59:59.999999999Z"), DateRangeParser.parseTo("2026-11-05").toInstant());
    }

    @Test
    public void parse_dateTimeWithOffsetIsKeptAsGivenInstantForBothBounds() {
        Instant expected = Instant.parse("2026-11-01T08:00:00Z");
        assertEquals(expected, DateRangeParser.parseFrom("2026-11-01T09:00:00+01:00").toInstant());
        assertEquals(expected, DateRangeParser.parseTo("2026-11-01T09:00:00+01:00").toInstant());
    }

    @Test
    public void parse_dateTimeWithZuluAndWithoutOffsetIsTreatedAsUtc() {
        assertEquals(Instant.parse("2026-11-01T09:00:00Z"), DateRangeParser.parseFrom("2026-11-01T09:00:00Z").toInstant());
        assertEquals(Instant.parse("2026-11-01T09:00:00Z"), DateRangeParser.parseFrom("2026-11-01T09:00:00").toInstant());
    }

    @Test
    public void parse_trimsSurroundingWhitespace() {
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), DateRangeParser.parseFrom(" 2026-11-01 ").toInstant());
    }

    @Test
    public void parse_nullOrBlankMeansNoBound() {
        assertNull(DateRangeParser.parseFrom(null));
        assertNull(DateRangeParser.parseFrom(""));
        assertNull(DateRangeParser.parseTo("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-date", "01-11-2026", "2026-13-01", "2026-02-30", "2026-11-01T25:00:00", "2026/11/01", "20261101"})
    public void parse_throwsInvalidDateRangeExceptionForMalformedValues(String value) {
        InvalidDateRangeException fromException = assertThrows(InvalidDateRangeException.class, () -> DateRangeParser.parseFrom(value));
        InvalidDateRangeException toException = assertThrows(InvalidDateRangeException.class, () -> DateRangeParser.parseTo(value));

        assertTrue(fromException.getMessage().contains("'from'"));
        assertTrue(toException.getMessage().contains("'to'"));
    }
}