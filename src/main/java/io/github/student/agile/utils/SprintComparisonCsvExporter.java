package io.github.student.agile.utils;

import io.github.student.agile.resource.dto.SprintComparisonItemDTO;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generator pliku CSV (RFC 4180) dla zestawienia porównującego sprinty w projekcie.
 */
@UtilityClass
public class SprintComparisonCsvExporter {

    private static final String LINE_SEPARATOR = "\r\n";
    private static final char FIELD_SEPARATOR = ',';
    private static final String HEADER_ROW = String.join(",",
            "projectId",
            "sprintId",
            "sprintName",
            "startDate",
            "endDate",
            "completedIssueCount",
            "uncompletedIssueCount",
            "velocity"
    );

    public static byte[] toCsvBytes(List<SprintComparisonItemDTO> items) {
        return toCsv(items).getBytes(StandardCharsets.UTF_8);
    }

    public static String toCsv(List<SprintComparisonItemDTO> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_ROW).append(LINE_SEPARATOR);

        if (items == null || items.isEmpty()) {
            return sb.toString();
        }

        for (SprintComparisonItemDTO item : items) {
            if (item == null) {
                continue;
            }
            sb.append(escapeCsv(formatInteger(item.getProjectId()))).append(FIELD_SEPARATOR)
                    .append(escapeCsv(formatInteger(item.getSprintId()))).append(FIELD_SEPARATOR)
                    .append(escapeCsv(item.getSprintName())).append(FIELD_SEPARATOR)
                    .append(escapeCsv(formatDate(item.getStartDate()))).append(FIELD_SEPARATOR)
                    .append(escapeCsv(formatDate(item.getEndDate()))).append(FIELD_SEPARATOR)
                    .append(item.getCompletedIssueCount()).append(FIELD_SEPARATOR)
                    .append(item.getUncompletedIssueCount()).append(FIELD_SEPARATOR)
                    .append(item.getVelocity())
                    .append(LINE_SEPARATOR);
        }

        return sb.toString();
    }

    static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.indexOf(FIELD_SEPARATOR) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;

        if (!needsQuoting) {
            return value;
        }

        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String formatInteger(Integer value) {
        return value != null ? value.toString() : "";
    }

    private static String formatDate(ZonedDateTime dateTime) {
        return dateTime != null ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime) : "";
    }
}
