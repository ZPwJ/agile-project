package io.github.student.agile.analytics;

import io.github.student.agile.resource.analytics.metrics.IssueOutcome;
import io.github.student.agile.resource.analytics.metrics.MetricUnit;
import io.github.student.agile.resource.analytics.metrics.SprintMetrics;
import io.github.student.agile.resource.analytics.metrics.SprintMetricsCalculator;
import io.github.student.agile.resource.analytics.metrics.SprintState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SprintMetricsCalculatorTest {

    private static SprintMetrics calc(IssueOutcome... outcomes) {
        return SprintMetricsCalculator.calculate(7, SprintState.FINISHED, List.of(outcomes));
    }

    @Test
    public void emptySprint_hasAllZeros() {
        SprintMetrics m = calc();

        assertEquals(0, m.totalTasks());
        assertEquals(0, m.completedTasks());
        assertEquals(0, m.incompleteTasks());
        assertEquals(0, m.unestimatedTasks());
        assertEquals(0, m.completedEstimate());
        assertEquals(0, m.incompleteEstimate());
        assertEquals(MetricUnit.STORY_POINTS, m.unit());
        assertEquals(7, m.sprintId());
        assertEquals(SprintState.FINISHED, m.state());
    }

    @Test
    public void onlyIncompleteTasks_doNotIncreaseVelocity() {
        SprintMetrics m = calc(new IssueOutcome(5, false), new IssueOutcome(3, false));

        assertEquals(0, m.completedTasks());
        assertEquals(2, m.incompleteTasks());
        assertEquals(0, m.completedEstimate());
        assertEquals(8, m.incompleteEstimate());
    }

    @Test
    public void completedTasks_sumIntoVelocity() {
        SprintMetrics m = calc(new IssueOutcome(5, true), new IssueOutcome(8, true), new IssueOutcome(2, false));

        assertEquals(3, m.totalTasks());
        assertEquals(2, m.completedTasks());
        assertEquals(1, m.incompleteTasks());
        assertEquals(13, m.completedEstimate());
        assertEquals(2, m.incompleteEstimate());
    }

    @Test
    public void unestimatedTasks_countAsTasksButAddNoPoints() {
        SprintMetrics m = calc(new IssueOutcome(null, true), new IssueOutcome(null, false), new IssueOutcome(4, true));

        assertEquals(3, m.totalTasks());
        assertEquals(2, m.completedTasks());
        assertEquals(1, m.incompleteTasks());
        assertEquals(2, m.unestimatedTasks());
        assertEquals(4, m.completedEstimate());
        assertEquals(0, m.incompleteEstimate());
    }

    @Test
    public void zeroEstimation_isEstimatedNotMissing() {
        SprintMetrics m = calc(new IssueOutcome(0, true));

        assertEquals(1, m.completedTasks());
        assertEquals(0, m.unestimatedTasks());
        assertEquals(0, m.completedEstimate());
    }

    @Test
    public void sums_doNotOverflowInt() {
        SprintMetrics m = calc(new IssueOutcome(Integer.MAX_VALUE, true), new IssueOutcome(Integer.MAX_VALUE, true));

        assertEquals(2L * Integer.MAX_VALUE, m.completedEstimate());
    }

    @Test
    public void sameInput_givesSameResult() {
        List<IssueOutcome> input = List.of(new IssueOutcome(3, true), new IssueOutcome(null, false));

        assertEquals(
                SprintMetricsCalculator.calculate(1, SprintState.ACTIVE, input),
                SprintMetricsCalculator.calculate(1, SprintState.ACTIVE, input));
    }
}
