# Background POST

> **Status: proof of concept.** Sending one dummy POST on a schedule exists to prove that scheduled background work, the terminal log and the backend connection all work. The real job will replace it. Treat the endpoint, the body and the 15-minute interval as placeholders.

## Goal
While the user has pressed **Start**, the phone sends a POST request to the backend about every 15 minutes, even when the app is closed. **Stop** cancels it.

## Behaviour
- **Start** schedules unique periodic work named `post_every_15_minutes` (15 minutes, requires a network connection). Pressing Start while the job exists changes nothing (`KEEP`).
- **Stop** cancels that work.
- Start is enabled only when no job exists; Stop only when one does. This comes from WorkManager's status, so it survives closing and reopening the app.
- Each run sends `POST https://component.wanlok.workers.dev/api/dummy` with body `{"name": "<Build.MODEL>"}`.
  - 2xx response: success.
  - Any other status or a network error: `Result.retry()` (WorkManager retries with backoff).

## Terminal log
Every event adds a line, formatted `HH:mm:ss  message`:
- `Started: POST every 15 minutes` and `Stopped`
- `POST <url>` just before each request, then `-> <status>` or `error: <message>`

Lines are kept in memory and in `terminal.log` (app private storage), last 500 lines, and reloaded when the app starts.

## Known limits
- 15 minutes is WorkManager's minimum and a target, not a promise: Doze, battery saver and some phone makers delay runs.
- Force-stopping or uninstalling the app cancels the job.
- Changing the job's settings (interval, constraints) does not apply to an existing job while `KEEP` is used. Press Stop then Start, or switch to `UPDATE`.
- The log file is rewritten on every line; fine for 500 lines.