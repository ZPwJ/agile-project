
package io.github.student.agile.resource.analytics.reports;

import io.github.student.agile.resource.dto.SprintComparisonRowDTO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class SprintComparisonCsvService {

    private static final String HEADER =
            "projectId,sprintId,sprintName,startDate,endDate,"
            + "completedTasks,incompleteTasks,velocity\r\n";

    public byte[] generate(List<SprintComparisonRowDTO> rows) {
        StringBuilder csv = new StringBuilder(HEADER);

        for (SprintComparisonRowDTO row : rows) {
            csv.append(cell(row.projectId())).append(",");
            csv.append(cell(row.sprintId())).append(",");
            csv.append(cell(row.sprintName())).append(",");
            csv.append(cell(row.startDate())).append(",");
            csv.append(cell(row.endDate())).append(",");
            csv.append(cell(row.completedTasks())).append(",");
            csv.append(cell(row.incompleteTasks())).append(",");
            csv.append(cell(row.velocity())).append("\r\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String cell(Object value) {
        if (value == null) {
            return "";
        }

        String text = value.toString();

        if (text.contains(",")
                || text.contains("\"")
                || text.contains("\n")
                || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
    }
}

