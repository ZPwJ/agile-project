package io.github.student.agile.analytics;

import java.time.format.DateTimeFormatter;
import java.util.List;

public final class SprintCsvWriter {

    private static final String LINE_END = "\r\n";
    private static final String HEADER =
            "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,incompleteIssueCount,velocity"
                    + LINE_END;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private SprintCsvWriter() {
    }

    public static String write(List<SprintComparisonDTO> rows) {
        StringBuilder content = new StringBuilder(HEADER);
        for (SprintComparisonDTO row : rows) {
            content.append(row.projectId()).append(',')
                    .append(row.sprintId()).append(',')
                    .append(escape(row.sprintName())).append(',')
                    .append(escape(formatDate(row.startDate()))).append(',')
                    .append(escape(formatDate(row.endDate()))).append(',')
                    .append(row.completedIssueCount()).append(',')
                    .append(row.incompleteIssueCount()).append(',')
                    .append(row.velocity())
                    .append(LINE_END);
        }
        return content.toString();
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String formatDate(java.time.ZonedDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }
}