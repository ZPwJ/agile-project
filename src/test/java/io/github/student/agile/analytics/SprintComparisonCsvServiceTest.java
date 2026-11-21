
package io.github.student.agile.analytics;

import io.github.student.agile.resource.analytics.reports.SprintComparisonCsvService;
import io.github.student.agile.resource.dto.SprintComparisonRowDTO;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SprintComparisonCsvServiceTest {

    private final SprintComparisonCsvService service =
            new SprintComparisonCsvService();

    private String generate(List<SprintComparisonRowDTO> rows) {
        return new String(
                service.generate(rows),
                StandardCharsets.UTF_8
        );
    }

    private SprintComparisonRowDTO row(String name) {
        return new SprintComparisonRowDTO(
                10,
                name,
                1,
                null,
                null,
                null,
                3,
                2,
                15L,
                null
        );
    }

    @Test
    void emptyReportContainsHeader() {
        String csv = generate(List.of());

        assertEquals(
                "projectId,sprintId,sprintName,startDate,endDate,"
                + "completedTasks,incompleteTasks,velocity\r\n",
                csv
        );
    }

    @Test
    void reportContainsSprintData() {
        String csv = generate(List.of(row("Sprint 1")));

        assertTrue(csv.contains(
                "1,10,Sprint 1,,,3,2,15\r\n"
        ));
    }

    @Test
    void commasAreEscaped() {
        String csv = generate(List.of(row("Sprint, pierwszy")));

        assertTrue(csv.contains("\"Sprint, pierwszy\""));
    }

    @Test
    void quotesAreEscaped() {
        String csv = generate(List.of(row("Sprint \"A\"")));

        assertTrue(csv.contains("\"Sprint \"\"A\"\"\""));
    }

    @Test
    void newlinesAreEscaped() {
        String csv = generate(List.of(row("Sprint\nDrugi")));

        assertTrue(csv.contains("\"Sprint\nDrugi\""));
    }

    @Test
    void nullDatesProduceEmptyCells() {
        String csv = generate(List.of(row("Sprint")));

        assertTrue(csv.contains("Sprint,,,3,2,15"));
    }

    @Test
    void rowsUseCrLf() {
        String csv = generate(List.of(row("Sprint")));

        assertTrue(csv.endsWith("\r\n"));
    }

    @Test
    void utf8CharactersArePreserved() {
        String csv = generate(List.of(row("Zażółć gęślą")));

        assertTrue(csv.contains("Zażółć gęślą"));
    }

    @Test
    void formulaLikeNamesAreNeutralised() {
        for (String name : List.of("=SUM(A1)", "+1", "-1", "@cmd", "	Tab")) {
            String csv = generate(List.of(row(name)));

            assertTrue(csv.contains(",'" + name + ","),
                    "not neutralised: " + name);
        }
    }

    @Test
    void ordinaryNamesAreNotPrefixed() {
        String csv = generate(List.of(row("Sprint 1")));

        assertFalse(csv.contains("'"));
    }

    @Test
    void datesAreWrittenAsOffsetDateTimeWithoutZoneName() {
        ZonedDateTime start = ZonedDateTime.of(
                2026, 2, 1, 9, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

        SprintComparisonRowDTO dated = new SprintComparisonRowDTO(
                10, "S", 1, start, null, null, 3, 2, 15L, null);

        assertTrue(generate(List.of(dated))
                .contains("1,10,S,2026-02-01T09:00:00+01:00,,3,2,15"));
    }
}
