package io.github.student.agile.resource.analytics.charts;

/**
 * Whether the recorded history covers the whole sprint
 * (docs/analytics/wf3-wf4-charts.md, dataCompleteness).
 * {@code PARTIAL} means "do not trust this chart as the whole truth", never "the chart is wrong".
 */
public enum SprintChartDataCompleteness {
    COMPLETE,
    PARTIAL
}
