package io.github.student.agile.resource.analytics.reports;

import io.github.student.agile.resource.dto.SprintComparisonRowDTO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SprintComparisonCsvService {

    private static final String HEADER =
            "projectId,sprintId,sprintName,startDate,endDate,"
            + "completedTasks,incompleteTasks,velocity";

    private static final String LINE_END = "\r\n";

    /** Characters that make Excel and similar tools evaluate a cell as a formula. */
    private static final String FORMULA_TRIGGERS = "=+-@\t\r";

    public byte[] generate(List<SprintComparisonRowDTO> rows) {
        String csv = Stream.concat(
                        Stream.of(HEADER),
                        rows.stream().map(this::line))
                .collect(Collectors.joining(LINE_END, "", LINE_END));

        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private String line(SprintComparisonRowDTO row) {
        return Stream.of(
                        cell(row.projectId()),
                        cell(row.sprintId()),
                        cell(row.sprintName()),
                        cell(row.startDate()),
                        cell(row.endDate()),
                        cell(row.completedTasks()),
                        cell(row.incompleteTasks()),
                        cell(row.velocity()))
                .collect(Collectors.joining(","));
    }

    private String cell(Object value) {
        if (value == null) {
            return "";
        }

        String text = value instanceof ZonedDateTime date
                ? date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : value.toString();

        if (value instanceof String && !text.isEmpty()
                && FORMULA_TRIGGERS.indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }

        if (text.contains(",")
                || text.contains("\"")
                || text.contains("\n")
                || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
    }
}
