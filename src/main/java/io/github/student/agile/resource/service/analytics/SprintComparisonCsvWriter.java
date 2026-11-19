package io.github.student.agile.resource.service.analytics;

import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO;
import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO.SprintResult;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Objects.isNull;

@Component
public class SprintComparisonCsvWriter {

    static final List<String> HEADER = List.of("projectID", "sprintID", "sprintName", "startedAt", "endedAt", "historyAvailable",
            "completedIssueCount", "uncompletedIssueCount", "velocity");

    private static final String SEPARATOR = ",";
    private static final String LINE_END = "\r\n";

    public String write(SprintComparisonDTO comparison) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, HEADER);
        List<SprintResult> sprints = isNull(comparison.getSprints()) ? List.of() : comparison.getSprints();
        for (SprintResult sprint : sprints) {
            appendRow(csv, List.of(
                    text(comparison.getProjectID()),
                    text(sprint.sprintID()),
                    text(sprint.sprintName()),
                    timestamp(sprint.startedAt()),
                    timestamp(sprint.endedAt()),
                    String.valueOf(sprint.historyAvailable()),
                    String.valueOf(sprint.completedIssueCount()),
                    String.valueOf(sprint.uncompletedIssueCount()),
                    String.valueOf(sprint.velocity())));
        }
        return csv.toString();
    }

    static String escape(String value) {
        if (value.contains(SEPARATOR) || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static void appendRow(StringBuilder csv, List<String> values) {
        csv.append(String.join(SEPARATOR, values.stream().map(SprintComparisonCsvWriter::escape).toList()))
                .append(LINE_END);
    }

    private static String text(Object value) {
        return isNull(value) ? "" : value.toString();
    }

    private static String timestamp(ZonedDateTime moment) {
        return isNull(moment) ? "" : DateTimeFormatter.ISO_INSTANT.format(moment);
    }
}
