package io.github.student.agile.service.analytics;

import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO;
import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO.SprintResult;
import io.github.student.agile.resource.service.analytics.SprintComparisonCsvWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SprintComparisonCsvWriterTest {

    private static final String HEADER =
            "projectID,sprintID,sprintName,startedAt,endedAt,historyAvailable,completedIssueCount,uncompletedIssueCount,velocity\r\n";

    private final SprintComparisonCsvWriter writer = new SprintComparisonCsvWriter();

    @Test
    void write_emptyComparison_returnsHeaderOnly() {
        assertEquals(HEADER, writer.write(comparison(List.of())));
    }

    @Test
    void write_withoutSprintList_returnsHeaderOnly() {
        assertEquals(HEADER, writer.write(comparison(null)));
    }

    @Test
    void write_writesOneRowPerSprintInGivenOrder() {
        SprintResult older = new SprintResult(4, "Sprint 2",
                ZonedDateTime.parse("2026-10-01T09:00:00+02:00"), ZonedDateTime.parse("2026-10-14T17:00:00+02:00"),
                true, 5, 1, 18);
        SprintResult newer = new SprintResult(5, "Sprint 3",
                ZonedDateTime.parse("2026-10-28T09:00:00+01:00"), ZonedDateTime.parse("2026-11-10T17:00:00+01:00"),
                true, 0, 3, 0);

        assertEquals(HEADER
                        + "1,4,Sprint 2,2026-10-01T07:00:00Z,2026-10-14T15:00:00Z,true,5,1,18\r\n"
                        + "1,5,Sprint 3,2026-10-28T08:00:00Z,2026-11-10T16:00:00Z,true,0,3,0\r\n",
                writer.write(comparison(List.of(older, newer))));
    }

    @Test
    void write_sprintWithoutHistory_leavesMissingDatesEmpty() {
        SprintResult withoutHistory = new SprintResult(1, "Legacy", null, null, false, 2, 0, 7);

        assertEquals(HEADER + "1,1,Legacy,,,false,2,0,7\r\n", writer.write(comparison(List.of(withoutHistory))));
    }

    @Test
    void write_escapesSprintName() {
        SprintResult sprint = new SprintResult(7, "Sprint \"Q4\", part 1", null, null, false, 0, 0, 0);

        assertEquals(HEADER + "1,7,\"Sprint \"\"Q4\"\", part 1\",,,false,0,0,0\r\n", writer.write(comparison(List.of(sprint))));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', quoteCharacter = '\'', value = {
            "Sprint 1            | Sprint 1",
            "''                  | ''",
            "a,b                 | '\"a,b\"'",
            "'say \"hi\"'        | '\"say \"\"hi\"\"\"'",
            "'\"'                | '\"\"\"\"'",
    })
    void escape_quotesOnlyValuesThatNeedIt(String value, String expected) {
        assertEquals(expected, escapeOf(value));
    }

    @Test
    void escape_quotesLineBreaks() {
        assertEquals("\"line 1\nline 2\"", escapeOf("line 1\nline 2"));
        assertEquals("\"line 1\r\nline 2\"", escapeOf("line 1\r\nline 2"));
        assertEquals("\"a\rb\"", escapeOf("a\rb"));
    }

    private String escapeOf(String value) {
        String csv = writer.write(comparison(List.of(new SprintResult(1, value, null, null, false, 0, 0, 0))));
        String row = csv.substring(HEADER.length(), csv.length() - "\r\n".length());
        return row.substring("1,1,".length(), row.length() - ",,,false,0,0,0".length());
    }

    private static SprintComparisonDTO comparison(List<SprintResult> sprints) {
        return SprintComparisonDTO.builder()
                .projectID(1)
                .timeZone("Europe/Warsaw")
                .estimationUnit("STORY_POINTS")
                .sprints(sprints)
                .build();
    }
}
