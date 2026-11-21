# Sprint analytics: shared DB schema

The schema is managed by Hibernate (`ddl-auto: update`; tests use H2 with `create-drop`). There are no
migration files, so **the entities are the schema** and file ownership replaces migration-number ranges.

## Changes in the foundation commit

| Change | Where | Notes |
|---|---|---|
| `Issue.estimation` rule | `IssueServiceImpl.validateEstimation` | Column already existed (`Integer`, nullable). `null` = not estimated. Negative values are rejected (`IllegalArgumentException`). The service checks it because there is no bean-validation provider on the classpath, so `@Min` would be a no-op. |
| `Sprint.endDate` | `Sprint`, `SprintDTO.endTime`, `SprintServiceImpl` | `ZonedDateTime`, nullable (same type as `startDate`). Accepted on create/update, must not be before `startDate`. Set to now by `stopSprint` if empty. |
| `issue_status_history` | `IssueStatusHistory`, `IssueStatusHistoryRepository`, `IssueServiceImpl.changeIssueStatus` | One row per accepted status change, written in the same transaction. |

### `issue_status_history`

| Column | Type | Notes |
|---|---|---|
| `HistoryID` | int, sequence | PK |
| `issue_id` | int, not null | plain id, no FK, so rows survive issue deletion |
| `sprintId` | int, nullable | plain id, no FK, so rows survive sprint deletion (`stopSprint` deletes the sprint) |
| `oldStatusId` | FK `issue_status` | |
| `newStatusId` | FK `issue_status`, not null | |
| `changedAt` | `Instant` (UTC), not null | convert to a calendar day (`Europe/Warsaw`) only when building charts |

Indexes: `(issue_id, changedAt)` and `(sprintId, newStatusId, changedAt)`.
History starts at deployment. Earlier changes are **not** back-filled (no invented data).

## Ownership (one owner per file, to avoid merge conflicts)

| Area | Owner |
|---|---|
| `Issue`, `Sprint` and estimation / end-date rules | A |
| `IssueStatusHistory*` and the hook in `changeIssueStatus` | B |
| `pom.xml`, JaCoCo, demo seed, this file | C |

Schema changes after this commit: add them in your own area and tell the other two. Never rewrite
another owner's entity without an Issue.

## Local DB

`docker compose up app-db` starts Postgres 16 (`agiledb`). If your local DB drifts, rebuild it:

```powershell
docker compose down -v
docker compose up -d app-db
```

## Finished sprints (`stopSprint` behaviour change)

Before: `stopSprint` deleted the sprint, detached its issues (`sprint = null`) and reset unfinished
issues to `TO_DO`, so nothing was left for reports. Now it **keeps the sprint** (`isDone = true`,
`active = false`, `endDate` set if empty) and first writes one `sprint_issue_snapshot` row per issue.
Issues are still detached and unfinished ones still reset to `TO_DO`, as before.
`deleteSprint` rejects finished sprints so reports cannot lose their data.

### `sprint_issue_snapshot`

| Column | Type | Notes |
|---|---|---|
| `SnapshotID` | int, sequence | PK |
| `SprintID` | FK `sprint`, not null | indexed |
| `issue_id` | int, not null | plain id, survives issue deletion |
| `estimation` | int, nullable | story points at stop time, `null` = not estimated |
| `finalStatusId` | FK `issue_status` | status before the reset to `TO_DO` |
| `completed` | boolean, not null | final status was `DONE` |

**Limitation (state it in the PR):** the snapshot reflects scope and estimations at the moment the
sprint was stopped. Issues removed from the sprint earlier, and estimation changes during the sprint,
are not recorded. Sprints stopped before this change were deleted and cannot be recovered.
