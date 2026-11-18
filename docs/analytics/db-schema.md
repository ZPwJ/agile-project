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

## Known limitation (needs a team decision)

`SprintServiceImpl.stopSprint` currently **deletes the sprint** and detaches its issues
(`sprint = null`; unfinished issues are reset to `TO_DO`). Reports for finished sprints
therefore have no sprint or membership data to read. See the PR discussion for the proposed fix.
