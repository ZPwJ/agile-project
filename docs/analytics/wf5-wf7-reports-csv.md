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

### CSV export
GET /api/analytics/projects/{projectId}/sprints/comparison.csv

The CSV endpoint accepts the same optional date filters.

Content-Type: text/csv;charset=UTF-8

CSV columns:
projectId,sprintId,sprintName,startDate,endDate,completedTasks,incompleteTasks,velocity

CSV follows RFC 4180 escaping rules, uses UTF-8 and CRLF line endings.

## Date filtering
- Dates use the Europe/Warsaw time zone.
- Filtering includes sprints overlapping the selected date range.
- Both date boundaries are inclusive.
- Invalid date formats and reversed ranges are rejected.

## Metrics
Sprint metrics are provided by SprintMetricsProvider.

Active and planned sprints use live issues.
Finished sprints use SprintIssueSnapshot records captured when a sprint is stopped.

## Demo data
The dev-only generator is SprintAnalyticsDemoData.

It prepares:
- Demo Alpha: 3 sprints
- Demo Beta: 3 sprints
- Completed, incomplete and unestimated issues
- Snapshots for finished sprints
- Issue status history with deterministic timestamps

The generator is enabled only with the dev Spring profile.

Development configuration uses the separate agiledb_demo PostgreSQL database.
The data-demo.sql script initializes the required issue statuses.

Do not enable the dev profile against a production database.

## Testing
Run:

mvn clean verify

The build includes unit and controller tests for sprint comparison and CSV export.

JaCoCo enforces at least 75% instruction coverage for the analytics.reports package.
This threshold does not represent coverage of the entire application.

## Integration
Stream C depends on SprintMetricsProvider from Stream A.
Integration with the other streams will be completed separately by the team.
