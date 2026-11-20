package io.github.student.agile.resource.analytics.charts;

/**
 * Whether the recorded history covers the whole sprint (plan section 3.5).
 * {@code PARTIAL} means "do not trust this chart as the whole truth", never "the chart is wrong".
 */
public enum SprintChartDataCompleteness {
    COMPLETE,
    PARTIAL
}
