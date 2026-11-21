# Stream C — Sprint comparison reports (WF5–WF7)

## Purpose
Stream C provides sprint comparison reports for a selected project, with optional date filtering and CSV export.

## Endpoints

### JSON comparison
GET /api/analytics/projects/{projectId}/sprints/comparison

Optional query parameters:
- from — ISO date, YYYY-MM-DD
- to — ISO date, YYYY-MM-DD

The response contains sprint identification, project ID, sprint dates, state, completed and incomplete task counts, velocity and metric unit.

Rows are sorted by `startDate` ascending (sprints without a start date last), then by `sprintId`.
Sprint dates are always returned in Europe/Warsaw (for example `2026-02-01T09:00:00+01:00`), whatever the database or JVM time zone is.
A project without matching sprints returns `200` with an empty array.

Errors use the analytics error body `{"code", "message", "timestamp"}`:

| Case | Status | `code` |
|---|---|---|
| `from`/`to` not in `yyyy-MM-dd`, or `from` after `to` | 400 | `INVALID_PARAMETER` |
| Unknown project | 404 | `NOT_FOUND` |

Parameters are validated before the project lookup, so an invalid range for an unknown project returns 400.

### CSV export
GET /api/analytics/projects/{projectId}/sprints/comparison.csv

The CSV endpoint accepts the same optional date filters.

Content-Type: text/csv;charset=UTF-8

CSV columns:
projectId,sprintId,sprintName,startDate,endDate,completedTasks,incompleteTasks,velocity

CSV follows RFC 4180 escaping rules, uses UTF-8 and CRLF line endings.

Dates are written as ISO offset date-times in Europe/Warsaw (`2026-02-01T09:00+01:00`); an open end date is an empty cell.
Text cells that start with `=`, `+`, `-`, `@`, a tab or a carriage return are prefixed with `'` so spreadsheet tools do not evaluate them as formulas.
The CSV does not contain `state` or `unit`; those are only in the JSON response.

## Date filtering
- Dates use the Europe/Warsaw time zone.
- Filtering includes sprints overlapping the selected date range.
- Both date boundaries are inclusive.
- Invalid date formats and reversed ranges are rejected.
- A sprint without a start date is excluded whenever `from` or `to` is set.
- A sprint without an end date (still running) matches any `from`.

## Metrics
Sprint metrics are provided by SprintMetricsProvider.

Active and planned sprints use live issues.
Finished sprints use SprintIssueSnapshot records captured when a sprint is stopped.

## Demo data
The demo-only generator is SprintAnalyticsDemoData.

It prepares:
- Demo Alpha: 3 sprints
- Demo Beta: 3 sprints
- Completed, incomplete and unestimated issues
- Snapshots for finished sprints
- Issue status history with deterministic timestamps

The generator is enabled only with the `demo` Spring profile.

The `demo` profile (`application-demo.yml`) uses the separate agiledb_demo PostgreSQL database.
The data-demo.sql script initializes the required issue statuses.

Do not enable the `demo` profile against a production database.
With this profile the JPA auditor is the fixed value `demo`.
The generator runs in one transaction and is skipped when a project with the same name already exists.

Run it with:

    mvn spring-boot:run "-Dspring-boot.run.profiles=demo"

## Testing
Run:

mvn clean verify

The build includes unit and controller tests for sprint comparison and CSV export.

JaCoCo enforces at least 75% instruction coverage for each of the analytics.reports and analytics.charts packages.
This threshold does not represent coverage of the entire application.

## Integration
Stream C depends on SprintMetricsProvider from Stream A.
Integration with the other streams will be completed separately by the team.
