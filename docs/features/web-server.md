# Web server

> **Status: API done, page pending.** The server serves `assets/index.html` (currently a "hello" page) and the task API. The desktop task page is the next step.

## Goal
Let a desktop browser on the same network add, list and delete scraping tasks. The tasks are stored on the phone only (`TaskRepository`).

## Behaviour
- **Start Web Server** (the Start item under the Web Server heading of the menu on Home) starts `ServerService`, a foreground service that runs a Ktor (CIO) server on port **8080**, listening on all interfaces. **Stop Web Server** stops it. This is separate from **Start Scraper** and **Stop Scraper**, which control the periodic job (see `background-job.md`), so tasks can be edited and `POST /api/run` used without scheduling.
- The service shows a notification with the address, for example `http://192.168.1.23:8080`. Tapping it opens the app.
- The address is also logged on Home: `STARTED  Web server http://192.168.1.23:8080`.
- `GET /` returns `app/src/main/assets/index.html`. Open the address in a desktop browser on the same Wi-Fi to check it works.
- Task API (JSON):
  - `GET /api/tasks` returns all tasks.
  - `GET /api/tasks/{id}` returns that task with its snapshots, newest first: `{ id, name, url, page_selector, data_selector, enabled, snapshots: [...] }`, with `page_selector` (only if the task has one) and `data_selector` exactly as they were sent. A completed snapshot is `{ date, status: "TASK_COMPLETED", data: [...] }` (see Data selector below) and a failed one is `{ date, status: "TASK_FAILED", error }`. `?limit=` sets how many snapshots (default 100, maximum 1000). A task that never ran has `"snapshots": []`. It returns 404 if there is no such task.
  - `POST /api/tasks` with `{ "name", "url", "page_selector", "data_selector", "enabled" }` adds a task and returns it (201). `page_selector` and `enabled` are optional. It returns 400 with `{ "error" }` if `name` or `data_selector` is blank, `url` is not an http(s) address, `enabled` is not `true` or `false`, or `page_selector` or `data_selector` is not valid (see below).
  - `PUT /api/tasks/{id}` with the same body replaces that task, keeping its id, and returns it (200). A `page_selector` that is left out is removed from the task, and an `enabled` that is left out becomes `true`. It has the same 400 errors, or returns 404 if there is no such task.
  - `DELETE /api/tasks/{id}` removes a task and its snapshots (204), or returns 404 if there is no such task.
- **Enabled** (optional boolean, default `true`). A task with `"enabled": false` is skipped by every run, the periodic job and `POST /api/run`. Skipping is not a failure: Home logs `TASK_SKIPPED` with the task name and no snapshot is saved. A run where every task is disabled just logs the skips and finishes. A task as the API returns it:
  ```json
  { "id": "…", "name": "CAP graphics cards", "url": "https://www.cap.com.hk/tc/products/category/graphics-card",
    "page_selector": { ".pagination a": "page" },
    "data_selector": { "div.product-card": { "name": ".card-title", "price": ".price-current" } },
    "enabled": true }
  ```
- **Page selector** (optional). `page_selector` is an object of one or more pairs, a CSS selector for the pagination links and the name of the page query parameter: `"page_selector": { ".pagination a": "page" }`. A run reads the task URL and every other page it finds, in page order, and puts all their data in one snapshot. Without it only the task URL is read. See Pagination below.
- **Data selector.** `data_selector` is either a string or an object, applied to every page:
  - A string is a CSS selector. Each matching element gives one text, so `data` is a list of strings: `"data_selector": "p.name"` gives `"data": ["First", "Second"]`.
  - An object has exactly one key, the container selector, whose value maps field names to CSS selectors searched inside each container. Each container gives one object, so `data` is a list of objects. A field that isn't found inside a container is `null`.
    ```json
    "data_selector": { "div.card": { "name": "h6.card-title", "price": ".price" } }
    ```
    ```json
    "data": [ { "name": "ASUS ROG ASTRAL", "price": "HK$ 12,999" }, { "name": "INNO 3D RTX5070", "price": null } ]
    ```
  - It returns 400 if the object has more or fewer than one key, if the container's value isn't an object or has no fields (use the string form), or if a field name or selector is blank.
- `POST /api/run` starts all enabled tasks now, the same code as the periodic job, and returns 202 at once without waiting for them to finish, since a run can take minutes. Progress and results appear in the Home log, and the data in the snapshots. It returns 409 `{ "error" }` if a run is already in progress. The run continues even if the server is stopped.
- On Android 13+, Start Web Server first asks for the notification permission. The server starts whether or not it is granted.

## Pagination
For a list that is spread over several pages, a task has a `page_selector`. The task URL is scraped as page 1, and the other pages are worked out from the pagination links on it. The page list function is `PageUrls` / `PageScraper.pageUrls`, and `POST /api/pages` tests it.

```json
"page_selector": { ".pagination a": "page" }
```
- The value is an object with one or more pairs. The key is a CSS selector for the pagination links, and the value is the name of the query parameter that holds the page number (`page`, `p`, …). A plain string is not valid (400), since it can't say which parameter to use.
- For each pair, the links its selector matches are filtered to those with exactly that parameter (`?page=N`, `&page=N`), with N of 1 or more. Other links (`#`, product links, `page=0`) are ignored. The largest N is the last page, and the pages 2 to N are the link with only the page value changed, so the other parameters are kept.
- With several pairs, the lists are joined in the order written and duplicates are removed. A pair that finds no link with its parameter fails the whole call (`no links with page parameter "p"`).
- There is no limit on the number of pages.
- The page list is built by one function that takes a URL and a `page_selector`. The test endpoint and the task run both use it.
- In a run, the pages are read in order with a pause of one second between them, and the items are joined into one snapshot. If a page fails to load or has an invalid data selector, the whole run fails and the reason names the page (`page 7: HTTP 500`). A page with no matching items ends the list. If that is the first page, the run fails with `data selector matched nothing`.

`POST /api/pages` is the test endpoint for it. It takes `{ "url", "page_selector" }` and returns `{ "pages": [ "<url>", "<url>?page=2", … ] }`. It loads the page in the WebView, so it takes a few seconds. It returns 400 for a bad body, and 422 with `{ "error" }` if the page can't be loaded (`HTTP 404`, `page took too long to load`, …), a selector is invalid (`invalid page_selector`) or matches nothing (`page_selector matched nothing: <selector>`, after waiting up to 10 seconds for it to appear), or no link has the parameter (`no links with page parameter "<param>"`).

**Assumption, to revisit:** the URL given for the task is always page 1 and is used as it is. If someone gives a later page, such as `?page=3`, that page is listed first and page 1 is missing. A safer version would remove the page parameter from the task URL to get page 1. It assumes a page without the parameter is page 1, which holds for the two sites tried (`cap.com.hk` and `shop.pegasus.hk`). Both sites were checked as plain HTML, with links such as `?page=2`.

## Planned
- A page with the task list and a form (name, URL, page selector, data selector), using the API.
- An access code, URL validation.
- Wake lock and Wi-Fi lock so the phone stays reachable with the screen off, as an option in Settings, plus an auto-stop timer.

## Known limits
- Same network only. Some routers block device-to-device traffic (client isolation).
- Plain HTTP and no authentication yet: anyone on the network can open the page.
- Without a wake lock, Doze can make the phone unreachable after the screen has been off for a while.
- If the app is killed while it is closed (swiping it away on Xiaomi/MIUI, for example), the web server stops and nothing can restart it until the app is opened again. The 15 minute job survives a swipe, so tasks keep running. Nothing restarts the server by itself: open the app and choose Start Web Server (it is enabled again, since the menu looks at whether the server is running).
- To make Xiaomi/MIUI kill the app less often, set the app to "No restrictions", enable Autostart, and lock the app in the recent apps list.