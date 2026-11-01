# Background job

> **Status: first version.** The dummy POST to the backend was removed. **Start** schedules a periodic job that runs the saved tasks, saves a snapshot of what it extracts and logs the outcome. Snapshots are served by the API; sending them to Firestore is not built yet.
## Goal
While the user has pressed **Start**, the phone runs a job about every 15 minutes, even when the app is closed. **Stop** cancels it.

## Behaviour
- **Start** schedules unique periodic work named `run_tasks_every_15_minutes` (15 minutes, requires a network connection). Pressing Start while the job exists changes nothing (`KEEP`).
- **Stop** cancels that work.
- Start is enabled only when no job exists; Stop only when one does. This comes from WorkManager's status, so it survives closing and reopening the app.
- Each run goes through the saved tasks one at a time (see Task run). The job itself always succeeds; failures are logged per task.

## Task run
For each saved task, in order:
1. Log `TASK_RUNNING` with the task name.
2. If the task has a `page_selector`, work out the URLs of all its pages first (see `web-server.md`, Pagination); otherwise the only page is the task URL. For each page in order (with a one second pause between pages), load the URL in a WebView (the system browser engine, so JavaScript runs like in Chrome; 30 second limit), then try the data selector (a CSS selector, run with `querySelectorAll`) every 250 ms, for up to 10 seconds, until something matches. This gives the page's scripts time to build the content. The result is a list in page order with one item per matching element: its text, or, if the data selector has fields, an object with the text of each field found inside that element (`null` if a field isn't found). See `web-server.md` for the format. It is taken the moment the first match appears, so a page that adds its items in several steps may return only the first batch.
3. Save a snapshot for the task (`TASK_COMPLETED` with the data, or `TASK_FAILED` with the error), and log `TASK_COMPLETED` with the task name, or `TASK_FAILED` with `<name>: <reason>`.

A task fails when the page fails to load (the error text, such as `net::ERR_NAME_NOT_RESOLVED`), the server returns an error status (`HTTP 404`), the page takes more than 30 seconds to load, the data selector is invalid (`invalid data selector`, or `invalid selector for field <name>`), or the data selector matches nothing within 10 seconds of the page loading (so such a task takes about 10 seconds to fail). An element whose text is empty still counts as completed (it adds an empty string to the list). One failing task does not stop the others. Snapshots are saved on the phone, one file per task (`snapshots/<id>.jsonl`, one JSON line per run, data kept in full). They are read with `GET /api/tasks/{id}` and deleted with the task. Nothing is sent to Firestore yet.

`POST /api/run` on the web server starts all tasks immediately, to test without waiting for the schedule. It returns at once (202) and the run goes on in the background. Only one run happens at a time: `/api/run` returns 409 while one is in progress, and a scheduled run that starts during another waits for it.

## Log
Every event adds a log `{ "date": <ISO 8601 date and time, e.g. "2026-10-31T11:07:08.123+11:00">, "status": <"STARTED" | "STOPPED" | "TASK_RUNNING" | "TASK_PAGES_FOUND" | "TASK_PAGE_COMPLETED" | "TASK_COMPLETED" | "TASK_FAILED">, "content": <string, optional> }`. The screen shows the time of day, as `HH:mm:ss  status  content`. The web server adds `STARTED` (content: its address) and `STOPPED` (content: `Web server`); each task run adds `TASK_RUNNING`, then `TASK_COMPLETED` or `TASK_FAILED`, with the task name (and the failure reason) as content. A task with a `page_selector` also adds `TASK_PAGES_FOUND` (content `<name>: 18 pages`) once the page list is known, and `TASK_PAGE_COMPLETED` (content `<name>: page 3/18, 12 items`) after each page:
```
10:21:02  TASK_RUNNING         Cards
10:21:06  TASK_PAGES_FOUND     Cards: 18 pages
10:21:09  TASK_PAGE_COMPLETED  Cards: page 1/18, 12 items
…
10:22:40  TASK_COMPLETED       Cards
``` This format is expected to change.

Logs are saved in one file per day, `logs/yyyyMMdd.txt` (app private storage), one JSON object per line. The screen shows today's logs and reloads today's file when the app starts. Old files are not deleted automatically. Lines in the old plain-text format are skipped when loading.

## Known limits
- 15 minutes is WorkManager's minimum and a target, not a promise: Doze, battery saver and some phone makers delay runs.
- Force-stopping or uninstalling the app cancels the job.
- Changing the job's settings (interval, constraints) does not apply to an existing job while `KEEP` is used. Press Stop then Start, or switch to `UPDATE`.
- Each day's log is read into memory in full when the app starts; fine at this size.