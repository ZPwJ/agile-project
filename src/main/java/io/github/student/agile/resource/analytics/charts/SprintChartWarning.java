package io.github.student.agile.resource.analytics.charts;

/** Stable machine-readable codes of the chart warnings. Order == order in the JSON array. */
public enum SprintChartWarning {

    NO_DATES("the sprint has no start date, so no point can be placed on the timeline"),
    INVALID_DATE_RANGE("the sprint end date is before its start date, so no point can be placed on the timeline"),
    NO_RECORDED_CHANGES("no status change is recorded inside the sprint dates"),
    HISTORY_STARTED_LATE("status history recording started after the sprint start, so earlier days are missing"),
    UNDATED_COMPLETION("%d completed task(s) have no recorded DONE change and are not counted on any day"),
    SCOPE_CHANGED("%d task(s) with recorded status changes are not in the sprint scope and are ignored"),
    UNESTIMATED_TASKS("%d task(s) have no estimation and count as 0 points"),
    EMPTY_SPRINT("the sprint has no tasks"),
    SPRINT_NOT_STARTED("the sprint has not started, the timeline covers its planned days only"),
    SPRINT_IN_PROGRESS("the sprint is still active, the timeline ends today"),
    SCOPE_NOT_HISTORICAL("scope changes during the sprint are not recorded, so 'scope' is the sprint scope when the numbers were taken");

    private final String text;

    SprintChartWarning(String text) {
        this.text = text;
    }

    /** "CODE: message", formatted with the given arguments (none for warnings without a count). */
    public String render(Object... args) {
        return name() + ": " + String.format(text, args);
    }
}
