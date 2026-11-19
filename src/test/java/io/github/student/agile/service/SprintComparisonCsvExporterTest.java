package io.github.student.agile.service;

import io.github.student.agile.resource.dto.SprintComparisonItemDTO;
import io.github.student.agile.utils.SprintComparisonCsvExporter;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SprintComparisonCsvExporterTest {

    private static final String EXPECTED_HEADER =
            "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,uncompletedIssueCount,velocity\r\n";

    @Test
    public void toCsv_returnsOnlyHeaderRowForEmptyOrNullList() {
        assertEquals(EXPECTED_HEADER, SprintComparisonCsvExporter.toCsv(Collections.emptyList()));
        assertEquals(EXPECTED_HEADER, SprintComparisonCsvExporter.toCsv(null));
        assertEquals(EXPECTED_HEADER, new String(SprintComparisonCsvExporter.toCsvBytes(Collections.emptyList()), StandardCharsets.UTF_8));
    }

    @Test
    public void toCsv_formatsRowsWithIsoOffsetDatesAndNullFields() {
        ZonedDateTime start1 = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end1 = ZonedDateTime.of(2026, 11, 5, 17, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime start2 = ZonedDateTime.of(2026, 11, 6, 9, 0, 0, 0, ZoneOffset.ofHours(1));

        List<SprintComparisonItemDTO> items = List.of(
                SprintComparisonItemDTO.builder()
                        .projectId(10)
                        .sprintId(1)
                        .sprintName("Sprint 1")
                        .startDate(start1)
                        .endDate(end1)
                        .completedIssueCount(4)
                        .uncompletedIssueCount(1)
                        .velocity(13)
                        .build(),
                SprintComparisonItemDTO.builder()
                        .projectId(10)
                        .sprintId(2)
                        .sprintName(null)
                        .startDate(start2)
                        .endDate(null)
                        .completedIssueCount(0)
                        .uncompletedIssueCount(2)
                        .velocity(0)
                        .build()
        );

        String expected = EXPECTED_HEADER
                + "10,1,Sprint 1,2026-11-01T09:00:00Z,2026-11-05T17:00:00Z,4,1,13\r\n"
                + "10,2,,2026-11-06T09:00:00+01:00,,0,2,0\r\n";

        assertEquals(expected, SprintComparisonCsvExporter.toCsv(items));
    }

    @Test
    public void toCsv_escapesCommasQuotesAndNewlinesAccordingToRfc4180() {
        ZonedDateTime start = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);

        List<SprintComparisonItemDTO> items = List.of(
                SprintComparisonItemDTO.builder()
                        .projectId(10)
                        .sprintId(1)
                        .sprintName("Sprint, 1 \"Alpha\"")
                        .startDate(start)
                        .endDate(null)
                        .completedIssueCount(2)
                        .uncompletedIssueCount(0)
                        .velocity(8)
                        .build(),
                SprintComparisonItemDTO.builder()
                        .projectId(10)
                        .sprintId(2)
                        .sprintName("Sprint\nMultiline\r\nName")
                        .startDate(null)
                        .endDate(null)
                        .completedIssueCount(1)
                        .uncompletedIssueCount(1)
                        .velocity(5)
                        .build()
        );

        String expected = EXPECTED_HEADER
                + "10,1,\"Sprint, 1 \"\"Alpha\"\"\",2026-11-01T09:00:00Z,,2,0,8\r\n"
                + "10,2,\"Sprint\nMultiline\r\nName\",,,1,1,5\r\n";

        assertEquals(expected, SprintComparisonCsvExporter.toCsv(items));
    }
}
