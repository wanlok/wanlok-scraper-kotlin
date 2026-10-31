# Background job

> **Status: first version.** The dummy POST to the backend was removed. **Start** schedules a periodic job that runs the saved tasks and logs what it extracts. Saving the results (Firestore) is not built yet. The class names (`PostWorker`, `PostScheduler`) are left over from the dummy POST and should be renamed.

## Goal
While the user has pressed **Start**, the phone runs a job about every 15 minutes, even when the app is closed. **Stop** cancels it.

## Behaviour
- **Start** schedules unique periodic work named `post_every_15_minutes` (15 minutes, requires a network connection). Pressing Start while the job exists changes nothing (`KEEP`).
- **Stop** cancels that work.
- Start is enabled only when no job exists; Stop only when one does. This comes from WorkManager's status, so it survives closing and reopening the app.
- Each run goes through the saved tasks one at a time (see Task run). The job itself always succeeds; failures are logged per task.

## Task run
For each saved task, in order:
1. Log `TASK_RUNNING` with the task name.
2. Fetch the URL with Jsoup (15 second timeout) and take the text of the first element matching the CSS selector.
3. Log `TASK_COMPLETED` with `<name>: <text>` (text shortened to 200 characters), or `TASK_FAILED` with `<name>: <reason>`.

A task fails when the request fails or times out, the server returns an error status (`HTTP 404`), the response is not HTML, the URL or the selector is invalid, or the selector matches nothing. An element whose text is empty still counts as completed. One failing task does not stop the others. The extracted text is only logged for now.

`POST /api/run` on the web server runs all tasks immediately, to test without waiting for the schedule.

## Log
Every event adds a log `{ "date": <ISO 8601 date and time, e.g. "2026-10-31T11:07:08.123+11:00">, "status": <"STARTED" | "STOPPED" | "TASK_RUNNING" | "TASK_COMPLETED" | "TASK_FAILED">, "content": <string, optional> }`. The screen shows the time of day, as `HH:mm:ss  status  content`. The web server adds `STARTED` (content: its address) and `STOPPED` (content: `Web server`); each task run adds `TASK_RUNNING`, then `TASK_COMPLETED` or `TASK_FAILED`, with the task name (and the extracted text or the failure reason) as content. This format is expected to change.

Logs are saved in one file per day, `logs/yyyyMMdd.txt` (app private storage), one JSON object per line. The screen shows today's logs and reloads today's file when the app starts. Old files are not deleted automatically. Lines in the old plain-text format are skipped when loading.

## Known limits
- 15 minutes is WorkManager's minimum and a target, not a promise: Doze, battery saver and some phone makers delay runs.
- Force-stopping or uninstalling the app cancels the job.
- Changing the job's settings (interval, constraints) does not apply to an existing job while `KEEP` is used. Press Stop then Start, or switch to `UPDATE`.
- Each day's log is read into memory in full when the app starts; fine at this size.