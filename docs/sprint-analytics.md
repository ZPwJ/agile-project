# Sprint analytics

Module that summarises sprint results: sprint report, velocity, burndown/burnup data,
sprint comparison with filtering and CSV export.

## 1. Assumptions

### 1.1. Completed issue
- An issue is **completed** only when its status is `DONE`.
- All other statuses (`TO_DO`, `IN_PROGRESS`, `CODE_REVIEW`, `IN_TESTS`) mean the issue is **not completed**.
- `DONE` is final: the application does not allow changing the status of a `DONE` issue
  (existing rule in `IssueServiceImpl`), so a completed issue never becomes uncompleted.

### 1.2. Estimation
- Unit: **story points** (`Issue.estimation`), returned in responses as `STORY_POINTS`.
- An issue **without an estimation** is counted in issue counts but adds **0 points** to every sum
  (velocity, remaining work, scope). It never causes an error.
- Reports use the estimation stored in the sprint history (`SprintIssueEvent.estimation`),
  not the current value of `Issue.estimation`.
- A completed issue counts with its estimation at the moment of completion (`COMPLETED` event);
  later estimation changes of that issue are ignored.

### 1.3. Sprint history
- Changes of an active sprint are stored as `SprintIssueEvent` records:
  `SCOPE_AT_START`, `ADDED`, `REMOVED`, `ESTIMATION_CHANGED`, `COMPLETED`.
- Scope changes during a sprint are taken from `ADDED` / `REMOVED` events.
- If a sprint has **no recorded history**, chart endpoints return an empty `points` list
  with `historyAvailable = false`. No data is guessed.

### 1.4. Dates
- Format: **ISO-8601** (`YYYY-MM-DD` for days, full timestamp with offset for date-times).
- Sprint days are calculated in **UTC**.

### 1.5. Date filter
- A sprint is included if it **overlaps** the `from`–`to` range (both bounds inclusive),
  based on the actual dates `startedAt` and `endDate`. A sprint without `endDate` is ongoing.
  A sprint that has not been started is not included.
- `from` and `to` are optional; `from` after `to` is rejected with HTTP 400.

### 1.6. API
- All new endpoints use the prefix `/api/analytics`.

## 2. Module skeleton

| Element | Class |
|---|---|
| Sprint metrics (counts, velocity) | `SprintMetricsCalculator` → `SprintMetrics` |
| Burndown / burnup points | `SprintChartCalculator` → `SprintChartDTO`, `ChartPointDTO` |
| Sprint comparison, CSV source | `SprintComparisonService` → `SprintComparisonRowDTO` |
| Sprint report | `SprintReportDTO` |
| Sprint history | `SprintIssueEvent`, `SprintEventType`, `SprintIssueEventRepository` |

## 3. Team workflow
- Shared branch: `feature/sprint-analytics-io74-g2`.
- Working branches: `task/<short-name>-io74-g2`.
- Commit messages: `type(scope): description (WF-x)`, e.g. `feat(analytics): add velocity calculation (WF-2)`.
- Everyone sets `git config user.name` and `git config user.email` matching their GitHub account.

## 4. Endpoints
_To be completed with each requirement (WF-1 – WF-7)._

## 5. Calculation rules
_To be completed: velocity, burndown, burnup._

## 6. Errors
_To be completed: HTTP status codes and error response format._

## 7. Limitations
_To be completed. Must include:_
- _Estimation changes of an issue after its completion are not reflected in reports (see 1.2)._
