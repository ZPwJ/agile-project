package io.github.student.agile.analytics;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Data point of the sprint burndown chart (WF-3).
 *
 * @param date          calendar day of the sprint point
 * @param remainingWork remaining work in story points at the end of the day (>= 0)
 * @param unit          unit of estimation, always STORY_POINTS
 */
public record BurndownPoint(
        LocalDate date,
        @JsonProperty("remainingWork")
        @JsonAlias({"remaining", "remainingPoints", "remainingEstimation"})
        int remainingWork,
        String unit) {

    public static final String STORY_POINTS = "STORY_POINTS";

    public BurndownPoint(LocalDate date, int remainingWork) {
        this(date, remainingWork, STORY_POINTS);
    }

    @JsonProperty("remaining")
    public int remaining() {
        return remainingWork;
    }
}
