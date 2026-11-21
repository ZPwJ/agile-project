# WF-3 and WF-4: sprint burndown and burnup charts

## Endpoints

| Method | Path | Response |
|---|---|---|
| GET | `/api/analytics/sprints/{sprintId}/burndown` | `BurndownDTO` |
| GET | `/api/analytics/sprints/{sprintId}/burnup` | `BurnupDTO` |

Both need a logged-in user (same as the rest of the API). A response contains the sprint's day
axis (normally one point per calendar day, empty for E1/E2), in `Europe/Warsaw`, with dates
formatted `yyyy-MM-dd`.

### Errors

The endpoints reuse the analytics error handling and never expose stack traces or internal
details. The body is an `ApiError` with exactly `code`, `message` and `timestamp`
(`timestamp` is an `OffsetDateTime`, serialized as ISO-8601 with offset):

| Case | Status | Body |
|---|---|---|
| unknown sprint | **404** | `{"code":"NOT_FOUND","message":"Sprint with ID <sprintId> does not exist","timestamp":"<ISO-8601 with offset>"}` |
| non-numeric `sprintId` | **400** | `{"code":"INVALID_PARAMETER","message":"Parameter 'sprintId' has an invalid value","timestamp":"<ISO-8601 with offset>"}` |

`AnalyticsExceptionHandler` is limited to `controller.analytics`, so the summary, velocity and
the other existing endpoints keep their behavior.

## Response fields

Common fields of both DTOs:

| Field | Type | Meaning |
|---|---|---|
| `sprintId` | `int` | the requested sprint; echoed back |
| `unit` | `MetricUnit` | always `STORY_POINTS` today; taken from `SprintMetricsProvider`, never recomputed |
| `dataCompleteness` | `SprintChartDataCompleteness` | `COMPLETE` or `PARTIAL` (see below) |
| `warnings` | array of strings | always present, possibly empty; `"CODE: message"`; deterministic order (see the catalogue) |
| `points` | array | one entry per axis day, ascending, no duplicates |

### `BurndownDTO` (WF-3)

`BurndownDTO(Integer sprintId, MetricUnit unit, SprintChartDataCompleteness dataCompleteness,
List<String> warnings, List<BurndownPointDTO> points)`

| Point field | Type | Meaning |
|---|---|---|
| `date` | `LocalDate` (`yyyy-MM-dd`) | calendar day in `Europe/Warsaw` |
| `remaining` | `long` | points still open at the end of the day: `max(0, scope - cumulative completed)`; never negative |

### `BurnupDTO` (WF-4)

`BurnupDTO(Integer sprintId, MetricUnit unit, SprintChartDataCompleteness dataCompleteness,
List<String> warnings, List<BurnupPointDTO> points)`

| Point field | Type | Meaning |
|---|---|---|
| `date` | `LocalDate` (`yyyy-MM-dd`) | calendar day in `Europe/Warsaw` |
| `completed` | `long` | cumulative completed points by the end of the day; may drop when a task is reopened |
| `scope` | `long` | sprint scope in points; the same value on every point of one response |

`unit` is always returned even though the only current value is `STORY_POINTS`.

## Rules

### Sources

| What | Where |
|---|---|
| `sprintId`, `unit` and the scope in points | `SprintMetricsProvider.forSprint`; `scope = completedEstimate + incompleteEstimate` |
| which tasks are in the sprint and their estimations | `sprint_issue_snapshot` for a `FINISHED` sprint, live `issue` rows for `PLANNED` / `ACTIVE` |
| when work was completed | `issue_status_history` filtered by the requested `sprintId`; this is the only source of "when" |
| when history recording started | `select min(changedAt) from issue_status_history`; history does not exist before this moment |

Completed means status `DONE`. Live issues are checked through `SprintCompletionPolicy`; a
finished sprint uses the snapshot's `completed` flag, written at stop time with the same `DONE`
comparison.

Nothing is invented: no synthetic points, no interpolation, no "assume it was done earlier".

### Day model

- A day is a calendar day in `Europe/Warsaw` (`ZoneId.of("Europe/Warsaw")`). Database timestamps
  stay `Instant` (UTC); `ZonedDateTime` is converted with
  `withZoneSameInstant(CHART_ZONE).toLocalDate()` and `Instant` with
  `LocalDate.ofInstant(instant, CHART_ZONE)`.
- The axis is inclusive: `startDate ... endDate`, one point per day, ascending, no duplicates.
  It uses `LocalDate` arithmetic, so a DST day is still exactly one point.
- A change is placed on its Warsaw day, not its UTC day. A change at `00:30Z` on a DST transition
  day belongs to that Warsaw day; a change at `23:30Z` belongs to the next Warsaw day.
- Effective end of the axis:
  - `ACTIVE`: `min(planned end, today)`, or today when there is no end date. If that is before the
    start, the axis is the single start day. Future days are never invented (E4).
  - `PLANNED`: the end date; without one, the axis is the single start day (E5).
  - `FINISHED`: the recorded end date; if missing, today (or the start day when today is earlier).
- No start date means no axis (E1). An effective end before the start means no axis (E2).

### Daily computation

1. Build the axis `start..end` as described above (E1–E5).
2. Read every status change of the sprint in one flat query, ordered by
   `issueId, changedAt, id`; keep only changes whose Warsaw day is inside the axis (R8).
3. For each kept change, `completed = SprintCompletionPolicy.isCompleted(newStatus)`.
   For one task the **last change of the day wins** (E11).
4. Walk the axis day by day. Each task's completed state is carried forward; the changes of the
   current day are applied at its end.
5. `cumulativeCompleted(day) = sum of estimations of tasks whose carried state is completed`.
6. `completed(day) = cumulativeCompleted(day) - cumulativeCompleted(previous day)`. It can be
   negative on a reopen (E11).
7. Burndown: `remaining(day) = max(0, scope - cumulativeCompleted(day))` (E13).
   Burnup: `completed = cumulativeCompleted(day)` and `scope = scope` (E12).

Burnup repeats the cumulative value, not the day's delta; the delta exists only inside
`SprintTimelineDay.completed`.

### `dataCompleteness`

`COMPLETE` only when all three hold:

- the axis exists (at least one day),
- history recording started on or before the sprint start day,
- no task is completed in the scope source whose last recorded change inside the axis is not
  `DONE` (E8).

Otherwise the response is `PARTIAL`. `PARTIAL` means "do not trust this chart as the whole truth",
never "the chart is wrong". State warnings such as `SPRINT_IN_PROGRESS`,
`SPRINT_NOT_STARTED` or `SCOPE_NOT_HISTORICAL` do not by themselves make the result `PARTIAL`.

### Warning catalogue

The order below is the enum declaration order and the order in the JSON array. Each code appears
at most once: the collector is an `EnumMap` with `putIfAbsent`. The rendered text is
`"CODE: message"`; `%d` is replaced by the count. The code before `": "` is stable and is part of
the API contract.

| Code | When (rule) | Fixed message |
|---|---|---|
| `NO_DATES` | E1 | the sprint has no start date, so no point can be placed on the timeline |
| `INVALID_DATE_RANGE` | E2 | the sprint end date is before its start date, so no point can be placed on the timeline |
| `NO_RECORDED_CHANGES` | no kept history row (E3) | no status change is recorded inside the sprint dates |
| `HISTORY_STARTED_LATE` | recording start after the sprint start (E3 / E6) | status history recording started after the sprint start, so earlier days are missing |
| `UNDATED_COMPLETION` | E8 | `%d` completed task(s) have no recorded DONE change and are not counted on any day |
| `SCOPE_CHANGED` | E7 | `%d` task(s) with recorded status changes are not in the sprint scope and are ignored |
| `UNESTIMATED_TASKS` | E9 | `%d` task(s) have no estimation and count as 0 points |
| `EMPTY_SPRINT` | E10 | the sprint has no tasks |
| `SPRINT_NOT_STARTED` | E5 | the sprint has not started, the timeline covers its planned days only |
| `SPRINT_IN_PROGRESS` | E4 | the sprint is still active, the timeline ends today |
| `SCOPE_NOT_HISTORICAL` | E12, only when points are returned | scope changes during the sprint are not recorded, so 'scope' is the sprint scope when the numbers were taken |

### Edge rules

| Id | Result |
|---|---|
| E1 | no `startDate` → empty `points`, `PARTIAL`, `NO_DATES`. |
| E2 | effective end before start → empty `points`, `PARTIAL`, `INVALID_DATE_RANGE`; an active sprint is clamped to a single start day instead (E4). |
| E3 | no kept change inside the sprint dates → `NO_RECORDED_CHANGES`; if recording covered the sprint start, a flat burndown at the scope, otherwise no points plus `HISTORY_STARTED_LATE` and `PARTIAL`. |
| E4 | `ACTIVE` → axis ends at `min(planned end, today)` (today without an end date); `SPRINT_IN_PROGRESS`; no future days. |
| E5 | `PLANNED` → axis `start .. (endDate ?? start)`; `SPRINT_NOT_STARTED`; in normal data the burndown stays at the full scope. |
| E6 | recording started after the sprint start → points begin at the first recorded change day; earlier days are omitted; `PARTIAL`, `HISTORY_STARTED_LATE`. |
| E7 | kept changes of tasks outside the current scope → ignored for points; `SCOPE_CHANGED` with the number of distinct such tasks. |
| E8 | a task completed in the scope source whose last recorded change inside the axis is not `DONE` → not counted on any day; `PARTIAL`, `UNDATED_COMPLETION` with the count. |
| E9 | a task without estimation → 0 points; `UNESTIMATED_TASKS` with the count. |
| E10 | no tasks → `EMPTY_SPRINT`. |
| E11 | completed and then reopened → `completed` drops from the reopen day (the day delta may be negative); `remaining` rises again. |
| E12 | scope is not historical → `SCOPE_NOT_HISTORICAL` whenever points are returned; burnup repeats the same `scope` on every point. |
| E13 | `scope - cumulative` would be negative → clamp burndown `remaining` to 0. |

## What cannot be reconstructed

- **Status history starts at deployment.** Work completed before the history table existed has no
  rows and is never back-filled. Such a task can appear as `UNDATED_COMPLETION` (E8), and the
  chart is `PARTIAL`.
- **Scope changes during the sprint are not recorded.** The scope is the value at stop time for a
  `FINISHED` sprint and the value now for a `PLANNED` / `ACTIVE` sprint. Burnup returns that value
  as a constant and flags `SCOPE_NOT_HISTORICAL` (E12).
- **Estimation changes during the sprint are not stored.** The chart uses the current estimation
  for an open sprint and the stop-time estimation for a finished sprint.
- **A task completed without a dated `DONE` change is not placed on any day (E8).** It is not
  counted in any daily point; the result is `PARTIAL` and the warning reports the number of tasks.
- **Tasks added or removed during the sprint are invisible in the daily points (E7).** Only
  changes of tasks in the current scope are priced; changes of other tasks are ignored and counted
  in `SCOPE_CHANGED`. Combined with the non-historical scope, the chart shows the scope when the
  numbers were taken, not how it evolved.

## Tasks without estimation and tasks of other sprints

- A task with `estimation = null` is still part of the sprint scope but adds 0 points. It produces
  `UNESTIMATED_TASKS` with the number of such tasks. An estimation of `0` is a real estimate and
  does not produce that warning.
- The history query is filtered by the requested `sprintId`; history rows of other sprints are
  never mixed into the chart. The scope source contains only tasks of the requested sprint. A row
  that belongs to the requested sprint but whose task is not in the current scope is ignored for
  points and counted in `SCOPE_CHANGED` (E7).

## Performance

The burndown and burnup endpoints run the same pipeline, once per request. A valid chart request
issues these reads:

| # | Read | Shape | Count |
|---|---|---|---|
| 1 | sprint for the chart service | `select ... from sprint where SprintID=?` | 1 |
| 2 | sprint for the metrics provider | `select ... from sprint where SprintID in (?)` | 1 |
| 3 | scope read by the provider (scope and unit) | snapshot rows for `FINISHED`, live issues otherwise, one query for the sprint | 1 |
| 4 | scope read by `SprintScopeLoader` (per-task estimations) | snapshot rows for `FINISHED`, live issues otherwise, by one sprint id | 1 |
| 5 | status history of the sprint | flat projection `issue_id, new_status_id, changed_at`, `where sprint_id=?`, ordered by `issue_id, changed_at, historyid` | 1 |
| 6 | status dictionary | `select statusid, status_name from issue_status` (five static rows) | 1 |
| 7 | recording start | `select min(changed_at) from issue_status_history` | 1 |

That is 2 sprint lookups, 2 scope reads, 2 history selects and 1 status-dictionary select: **7
selects** for a valid request. The status dictionary is static reference data. There is no query
per task and no query per history row: all tasks are read in one scope query, and all status
changes of the sprint in one flat history query. Grouping into Warsaw days happens in Java,
because grouping in SQL would mix the UTC calendar with the Warsaw calendar and lose the
"last change of the day" ordering. The `min(changed_at)` query is global and runs once per
request, not once per sprint.

The performance budget is one scope read per source; the code performs two single-scope reads (the
provider's internal read plus the loader's). This is still one query per source, not an N+1.
Requests that fail E1/E2 return before the history reads (4 selects); an unknown sprint throws
after the first sprint lookup (1 select).

## Examples

Both examples reproduce fixtures of `SprintChartsRestControllerTest`. In the captured test run
the fresh H2 database assigned `sprintId` 1 to the fixture; the endpoint always echoes the
requested id. The sprint is `2026-11-02 .. 2026-11-06`, active, so the responses carry
`SPRINT_IN_PROGRESS` and, because points are returned, `SCOPE_NOT_HISTORICAL`.

### Burndown

From `burndown_knownSprint_remainingDecreasesAfterWorkIsDone` (one 5-point task completed on
`2026-11-04`; the dates are asserted by `burndown_returnsAscendingPointsWithContractFields`):

```json
{
  "sprintId": 1,
  "unit": "STORY_POINTS",
  "dataCompleteness": "COMPLETE",
  "warnings": [
    "SPRINT_IN_PROGRESS: the sprint is still active, the timeline ends today",
    "SCOPE_NOT_HISTORICAL: scope changes during the sprint are not recorded, so 'scope' is the sprint scope when the numbers were taken"
  ],
  "points": [
    { "date": "2026-11-02", "remaining": 5 },
    { "date": "2026-11-03", "remaining": 5 },
    { "date": "2026-11-04", "remaining": 0 },
    { "date": "2026-11-05", "remaining": 0 },
    { "date": "2026-11-06", "remaining": 0 }
  ]
}
```

### Burnup

From `burnup_completedIsCumulativeAndScopeIsConstant`. The fixture has the same 5-point task and
adds a second 3-point task completed on `2026-11-05`, so the scope is 8:

```json
{
  "sprintId": 1,
  "unit": "STORY_POINTS",
  "dataCompleteness": "COMPLETE",
  "warnings": [
    "SPRINT_IN_PROGRESS: the sprint is still active, the timeline ends today",
    "SCOPE_NOT_HISTORICAL: scope changes during the sprint are not recorded, so 'scope' is the sprint scope when the numbers were taken"
  ],
  "points": [
    { "date": "2026-11-02", "completed": 0, "scope": 8 },
    { "date": "2026-11-03", "completed": 0, "scope": 8 },
    { "date": "2026-11-04", "completed": 5, "scope": 8 },
    { "date": "2026-11-05", "completed": 8, "scope": 8 },
    { "date": "2026-11-06", "completed": 8, "scope": 8 }
  ]
}
```

For the same test sprint with only the original 5-point task, the burnup would show
`completed` `0, 0, 5, 5, 5` with `scope: 5` on every point; the two examples in the tests cover
both the one-task and the two-task scenario.

## Code map

| Class | Role |
|---|---|
| `analytics.charts.SprintChartRepository` | flat status-history projection and the global recording-start query |
| `analytics.charts.SprintScopeLoader` | sprint tasks and estimations from the snapshot or live issues, one query per call |
| `analytics.charts.SprintTimelineService` | day axis, daily completed state, warnings and completeness; owns the E1–E12 rules |
| `analytics.charts.SprintChartsService` | maps `SprintTimeline` to the DTOs; applies the E13 burndown clamp |
| `analytics.charts.SprintTimeline`, `SprintTimelineDay`, `SprintTaskPoints` | internal value objects of the timeline |
| `analytics.charts.SprintChartWarning` | stable warning codes and messages; enum order is the JSON order |
| `analytics.charts.SprintChartWarnings` | `EnumMap` collector; deduplicates and orders warnings |
| `analytics.charts.SprintChartDataCompleteness` | `COMPLETE` / `PARTIAL` |
| `dto.BurndownDTO`, `dto.BurndownPointDTO` | WF-3 payload |
| `dto.BurnupDTO`, `dto.BurnupPointDTO` | WF-4 payload |
| `controller.analytics.SprintChartsRestController` | the two `GET` endpoints |
| `analytics.metrics.SprintMetricsProvider` | single source of scope and unit; never recomputed by charts |
| `analytics.metrics.SprintCompletionPolicy` | `DONE`-only completion rule |
| `analytics.error.AnalyticsExceptionHandler` | 404/400 mapping, limited to `controller.analytics` |
