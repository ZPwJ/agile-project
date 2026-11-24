package io.github.student.agile.analytics;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintCsvWriterTest {

    @Test
    void emptyResultContainsHeaderOnly() {
        String csv = SprintCsvWriter.write(List.of());
        assertEquals(
                "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,incompleteIssueCount,velocity\r\n",
                csv);
    }

    @Test
    void escapesCommasQuotesAndNewlines() {
        assertEquals("plain", SprintCsvWriter.escape("plain"));
        assertEquals("\"a,b\"", SprintCsvWriter.escape("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", SprintCsvWriter.escape("say \"hi\""));
        assertEquals("\"line1\rline2\"", SprintCsvWriter.escape("line1\rline2"));
    }

    @Test
    void writeEscapesSprintName() {
        SprintComparisonDTO row = new SprintComparisonDTO(
                1,
                2,
                "Sprint, \"Q1\"",
                ZonedDateTime.of(2024, 1, 5, 10, 0, 0, 0, ZoneOffset.UTC),
                null,
                SprintComparisonStatus.PLANNED,
                1,
                0,
                3,
                "STORY_POINTS");
        String csv = SprintCsvWriter.write(List.of(row));
        assertTrue(csv.startsWith(
                "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,incompleteIssueCount,velocity\r\n"));
        assertTrue(csv.contains("\"Sprint, \"\"Q1\"\"\""));
        assertTrue(csv.contains("\r\n"));
        assertTrue(csv.endsWith("\r\n"));
    }
}