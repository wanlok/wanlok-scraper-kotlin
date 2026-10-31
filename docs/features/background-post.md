# Background job

> **Status: placeholder.** The dummy POST to the backend was removed. **Start** still schedules a periodic job, but the job does nothing yet. Running the saved tasks will go here, and this feature will be rewritten then.

## Goal
While the user has pressed **Start**, the phone runs a job about every 15 minutes, even when the app is closed. **Stop** cancels it.

## Behaviour
- **Start** schedules unique periodic work named `post_every_15_minutes` (15 minutes, requires a network connection). Pressing Start while the job exists changes nothing (`KEEP`).
- **Stop** cancels that work.
- Start is enabled only when no job exists; Stop only when one does. This comes from WorkManager's status, so it survives closing and reopening the app.
- Each run currently does nothing and succeeds.

## Log
Every event adds a log `{ "date": <ISO 8601 date and time, e.g. "2026-10-31T11:07:08.123+11:00">, "status": <"STARTED" | "STOPPED" | "TASK_RUNNING" | "TASK_COMPLETED" | "TASK_FAILED">, "content": <string, optional> }`. The screen shows the time of day, as `HH:mm:ss  status  content`. Start and Stop add `STARTED` and `STOPPED` with no content; task runs will add the `TASK_` ones, with the task name as content. This format is expected to change.

Logs are saved in one file per day, `logs/yyyyMMdd.txt` (app private storage), one JSON object per line. The screen shows today's logs and reloads today's file when the app starts. Old files are not deleted automatically. Lines in the old plain-text format are skipped when loading.

## Known limits
- 15 minutes is WorkManager's minimum and a target, not a promise: Doze, battery saver and some phone makers delay runs.
- Force-stopping or uninstalling the app cancels the job.
- Changing the job's settings (interval, constraints) does not apply to an existing job while `KEEP` is used. Press Stop then Start, or switch to `UPDATE`.
- Each day's log is read into memory in full when the app starts; fine at this size.