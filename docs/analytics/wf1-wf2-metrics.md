# WF-1 and WF-2: sprint summary and velocity

## Endpoints

| Method | Path | Response |
|---|---|---|
| GET | `/api/analytics/sprints/{sprintId}/summary` | `SprintSummaryDTO` |
| GET | `/api/analytics/sprints/{sprintId}/velocity` | `VelocityDTO` |

Both need a logged-in user (same as the rest of the API). Unknown sprint: **404**. Non-numeric id: **400**.
Error body (no stack trace): `{"code": "NOT_FOUND" | "INVALID_PARAMETER", "message": "...", "timestamp": "<ISO-8601 with offset>"}`.
Dates are ISO-8601 with offset, e.g. `2026-11-18T10:15:30+01:00`.

### Summary response

| Field | Meaning |
|---|---|
| `sprintId`, `sprintName`, `projectId` | identity of the sprint |
| `startDate`, `endDate` | `null` when not set |
| `state` | `PLANNED` (not started), `ACTIVE`, `FINISHED` |
| `unit` | always `STORY_POINTS` |
| `totalTasks` | tasks in the sprint when the numbers were taken (see below) |
| `completedTasks`, `incompleteTasks` | `totalTasks` split by completion |
| `unestimatedTasks` | tasks with no estimation (already included in the counts above) |
| `velocity` | sum of estimations of completed tasks |
| `incompleteEstimate` | sum of estimations of incomplete tasks |

`velocity` endpoint returns `sprintId`, `state`, `unit`, `velocity`. Both read the same calculation,
so the numbers always agree.

## Rules

- **Completed** = status `DONE`. It is the terminal status of the existing flow
  (TO_DO, IN_PROGRESS, CODE_REVIEW, IN_TESTS, DONE). The rule lives in `SprintCompletionPolicy`.
- **No estimation** (`null`): the task counts in the task counts and in `unestimatedTasks`, adds 0 points.
  An estimation of `0` is a real estimate. Negative estimations are rejected when issues are saved.
- **Source of data:**
  - `FINISHED` sprint: the stop-time snapshot (`sprint_issue_snapshot`), because stopping a sprint detaches its issues.
  - `PLANNED` / `ACTIVE` sprint: live issues of the sprint.
- **Empty sprint**: all zeros, no error.
- Calculations are deterministic for the same data.

## Limitations

- `totalTasks` is the scope **when the numbers were taken** (at stop time for finished sprints, now otherwise).
  Tasks added or removed during the sprint are not recorded.
- Reports use the estimations at that moment; estimation changes during the sprint are not stored.
- Sprints stopped before the snapshot table existed were deleted by the old behaviour and cannot be reported.

## Code map

| Class | Role |
|---|---|
| `analytics.metrics.SprintMetricsCalculator` | pure counting rules |
| `analytics.metrics.SprintMetricsService` | reads sprint data (one query per source, any number of sprints); implements `SprintMetricsProvider` |
| `analytics.metrics.SprintReportService` | builds the DTOs, no counting logic |
| `controller.analytics.SprintMetricsRestController` | the two endpoints |
| `analytics.error.*` | 404/400 mapping, limited to `controller.analytics` |

Other streams use `SprintMetricsProvider` (`forSprint`, `forSprints`) and never recompute these numbers.
