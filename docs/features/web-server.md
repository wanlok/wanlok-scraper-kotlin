# Web server

> **Status: API done, page pending.** The server serves `assets/index.html` (currently a "hello" page) and the task API. The desktop task page is the next step.

## Goal
Let a desktop browser on the same network add, list and delete scraping tasks. The tasks are stored on the phone only (`TaskRepository`).

## Behaviour
- **Start** also starts `ServerService`, a foreground service that runs a Ktor (CIO) server on port **8080**, listening on all interfaces. **Stop** stops it.
- The service shows a notification with the address, for example `http://192.168.1.23:8080`. Tapping it opens the app.
- The address is also logged on Home: `STARTED  Web server http://192.168.1.23:8080`.
- `GET /` returns `app/src/main/assets/index.html`. Open the address in a desktop browser on the same Wi-Fi to check it works.
- Task API (JSON):
  - `GET /api/tasks` returns all tasks.
  - `POST /api/tasks` with `{ "name", "url", "selector" }` adds a task and returns it (201). It returns 400 with `{ "error" }` if `name` or `selector` is blank, or `url` is not an http(s) address.
  - `PUT /api/tasks/{id}` with the same body replaces that task, keeping its id, and returns it (200). It has the same 400 errors, or returns 404 if there is no such task.
  - `DELETE /api/tasks/{id}` removes a task (204), or returns 404 if there is no such task.
- `POST /api/run` runs all saved tasks now, the same code as the periodic job, and returns 204 when they have finished. Results appear in the Home log.
- On Android 13+, Start first asks for the notification permission. The server starts whether or not it is granted.

## Planned
- A page with the task list and a form (name, URL, selector), using the API.
- An access code, URL validation.
- Wake lock and Wi-Fi lock so the phone stays reachable with the screen off, as an option in Settings, plus an auto-stop timer.

## Known limits
- Same network only. Some routers block device-to-device traffic (client isolation).
- Plain HTTP and no authentication yet: anyone on the network can open the page.
- Without a wake lock, Doze can make the phone unreachable after the screen has been off for a while.
- The Start/Stop icon state comes from the WorkManager job, not from the server. If the system kills the service, the icons still show "running".
- Xiaomi/MIUI may kill the service; set the app to "No restrictions" and enable Autostart.