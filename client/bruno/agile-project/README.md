# Bruno collection: Agile Project

Requests and tests for the sprint analytics API: summary, velocity, burndown, burnup,
sprint comparison and CSV export.

## Use

1. Install [Bruno](https://www.usebruno.com/) and open this folder (`client/bruno/agile-project`) as a collection.
2. Select the **local** environment.
3. Create the credentials file: copy `.env.example` to `.env` (in this folder, next to `bruno.json`) and set
   `BRUNO_CLIENT_SECRET` and `BRUNO_PASSWORD`. Values: client secret and password of the sample account,
   see `client/http/http-client.env.json` (section `dev`). `.env` is ignored by git, so no secret is committed.
   Bruno reads it when the collection is opened (reload the collection after changing it), and the Bruno CLI reads it too.
   PowerShell shortcut, from the repository root:

   ```powershell
   $d = (Get-Content client/http/http-client.env.json -Raw | ConvertFrom-Json).dev
   "BRUNO_CLIENT_SECRET=$($d.clientSecret)`nBRUNO_PASSWORD=$($d.password)" | Set-Content -Encoding ascii client/bruno/agile-project/.env
   ```

4. Start the app with demo data (see the main README, "Demo data").
   You do not have to fetch a token by hand: a collection-level pre-request script (`collection.bru`)
   gets one automatically before any request when there is none or when it is about to expire
   (Keycloak tokens last about 5 minutes), so single requests and long runs both keep working.
   **Auth / Get token** is still there to inspect the token response.
5. Run a folder or the whole collection. The requests assume the demo data:
   project `1` (Demo Alpha) with finished sprints `1` and `2` and an active sprint `3`.
   Change `projectId` and `sprintId` in the environment to test other data.

## Folders

| Folder | Content |
|---|---|
| Auth | Token request, and a check that a request without a token gets 401 |
| Analytics | Happy-path requests with assertions on shape, ordering and consistency |
| Errors | 400 and 404 responses with the analytics error body |
| Scenarios | End-to-end flows that create their own data through the API, then check the reports |

Run **Sprint velocity** before **Burnup chart** (the default order does this):
the burnup test compares its last point with the velocity.

## Scenarios

Each scenario folder is independent. Its first request (**Get token**) also sets the unique `runId`
(and the dates used by the filter scenario); the token itself is refreshed automatically by the
collection script. Run a folder from top to bottom with
**Run** on the folder; the steps pass ids to each other through runtime variables.
Names are made unique with a `runId`, so a scenario can be repeated.

| Scenario | What it proves |
|---|---|
| 1 Finished sprint report | Create project, sprint and three issues (5 points, 3 points, no estimation), start the sprint, move one issue through the status flow to DONE, stop the sprint. Then: summary (live and after stop), velocity `5`, burndown ends at `3`, burnup ends at `5` of scope `8` and equals the velocity, comparison, CSV with a sprint name containing a comma, quotes and a leading `=` (escaped and neutralised), and a finished sprint cannot be deleted |
| 2 Guards and edge cases | Empty planned sprint (zero values, charts without points and a `NO_DATES` warning), starting an empty sprint, negative estimation and a status change outside an active sprint are rejected, a project without sprints gives an empty comparison and a header-only CSV |
| 3 Project isolation and date filter | Two projects with one finished sprint each: comparison and CSV only contain their own project; the date filter includes an overlapping sprint and excludes one in 2100; a reversed range gives 400 |

Notes:

- The scenarios **write data** (projects named `Scenario <runId> ...`) into the database the application uses.
  Use them against a local or demo database only. There is no cleanup step, because finished sprints cannot be deleted.
- The existing endpoints report business-rule violations (empty sprint, negative estimation, status flow,
  deleting a finished sprint) as HTTP 500 with a message. The scenario tests therefore check for an error status
  (400 or above) and the message text, not for a specific code.

With the Bruno CLI, from this folder:
`bru run --env local`
