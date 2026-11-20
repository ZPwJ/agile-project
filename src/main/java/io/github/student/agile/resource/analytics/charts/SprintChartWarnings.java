package io.github.student.agile.resource.analytics.charts;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Collects at most one entry per code, in enum order, so the JSON is deterministic. */
final class SprintChartWarnings {

    private final Map<SprintChartWarning, String> messages = new EnumMap<>(SprintChartWarning.class);

    void add(SprintChartWarning warning) {
        messages.putIfAbsent(warning, warning.render());
    }

    void add(SprintChartWarning warning, Object... args) {
        messages.putIfAbsent(warning, warning.render(args));
    }

    List<String> toList() {
        return List.copyOf(messages.values());
    }
}
