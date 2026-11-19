package io.github.student.agile.utils;

import io.github.student.agile.exception.definition.InvalidDateRangeException;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.INVALID_DATE_FORMAT;

/**
 * Parsuje opcjonalne parametry zakresu dat (ISO-8601) przekazywane w zapytaniach analitycznych.
 * <ul>
 *     <li>data bez czasu ({@code yyyy-MM-dd}): dla {@code from} początek dnia UTC, dla {@code to} koniec dnia UTC (włącznie),</li>
 *     <li>data z czasem: używana bez zmian; brak offsetu oznacza UTC,</li>
 *     <li>null lub pusty tekst: brak ograniczenia (zwracane jest {@code null}).</li>
 * </ul>
 */
@UtilityClass
public class DateRangeParser {

    public static final String FROM_PARAM = "from";
    public static final String TO_PARAM = "to";

    public static ZonedDateTime parseFrom(String raw) {
        return parse(raw, FROM_PARAM, false);
    }

    public static ZonedDateTime parseTo(String raw) {
        return parse(raw, TO_PARAM, true);
    }

    private static ZonedDateTime parse(String raw, String paramName, boolean endOfDayForDateOnly) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();

        try {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return endOfDayForDateOnly
                    ? date.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC)
                    : date.atStartOfDay(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // nie jest to sama data - próbujemy formatu data + czas
        }

        try {
            TemporalAccessor parsed = DateTimeFormatter.ISO_DATE_TIME
                    .parseBest(value, ZonedDateTime::from, LocalDateTime::from);
            if (parsed instanceof ZonedDateTime zonedDateTime) {
                return zonedDateTime;
            }
            return ((LocalDateTime) parsed).atZone(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new InvalidDateRangeException(String.format(INVALID_DATE_FORMAT, paramName));
        }
    }
}