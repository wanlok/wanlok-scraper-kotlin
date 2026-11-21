# Architecture

Single-Activity Jetpack Compose app (Material 3). Package: `com.wanlok.scraper`.

The app is at an early stage: the periodic job in `work/` runs the saved tasks, saves a snapshot of what it extracts and logs the outcome; sending the snapshots to Firestore is not built yet.
SDK: `minSdk` 24, `targetSdk` 36, `compileSdk` 37.

## Layout

```
com.wanlok.scraper
├── MainActivity.kt        entry point: setContent { ScraperTheme { AppNavigation() } }
├── ui
│   ├── navigation/        AppNavigation.kt (NavigationSuiteScaffold + NavHost), AppDestination.kt (top-level screens), Routes.kt
│   ├── components/        WListItem.kt (a tappable list row), SwipeToDelete.kt (swipe a row left to reveal Delete)
│   ├── landing/           LandingScreen.kt (the day row + log, and a top bar menu to start and stop the web server and the scraper), LogViewer.kt, DateScreen.kt (the Dates page), LogDays.kt (day text helpers)
│   ├── tasks/             TasksScreen.kt (read-only list of the saved tasks)
│   ├── settings/          SettingsScreen.kt (list of settings, saved automatically), ServerIpAddressesScreen.kt (the Server IP Addresses page)
│   └── theme/             Color.kt, Theme.kt, Type.kt
├── work
│   ├── TaskScheduler.kt   starts/stops the periodic job, exposes its running state
│   ├── TaskWorker.kt      one run of the job: runs all the saved tasks
│   ├── PageScraper.kt     loads a page in a WebView (runs its JavaScript) and reads the text of the matching elements, or the pagination links
│   ├── PageUrls.kt        works out the URL of every page of a list from its pagination links (no WebView, unit tested)
│   └── TaskRunner.kt      scrapes each task's pages, saves one snapshot, logs the outcome
├── server
│   ├── ServerService.kt   foreground service that runs the Ktor web server (port from Settings, 8080 by default)
│   ├── TaskRoutes.kt      the /api/tasks endpoints (list, add, edit, delete)
│   └── NetworkAddress.kt  the phone's local IPv4 address
└── data
    ├── WLog.kt                one log: { date, status, content? }
    ├── LogStatus.kt           STARTED, STOPPED, TASK_RUNNING, TASK_PAGES_FOUND, TASK_PAGE_COMPLETED, TASK_COMPLETED, TASK_FAILED or TASK_SKIPPED
    ├── LogRepository.kt       today's logs shown on screen, saved as JSON lines, one file per day; also reads, lists and deletes the files of other days
    ├── IpAddresses.kt         the phone's IPv4 addresses with their interface names
    ├── SettingsRepository.kt  saved settings (DataStore Preferences)
    ├── Task.kt                a scraping task: id, name, url, pageSelector (optional), dataSelector (text extraction only), scheduled (false = the periodic job skips it)
    ├── Scheduled.kt           reads the scheduled field of the API body (left out = true, not a boolean = error)
    ├── PageSelector.kt        how to find the pages of a list: pagination link selector -> page query parameter
    ├── DataSelector.kt        what a task reads: one CSS selector, or a container with named fields
    ├── TaskRepository.kt      the saved tasks (tasks.json)
    ├── Snapshot.kt            what one run of a task produced: { date, status, data (a list of texts or of field objects) or error }
    ├── SnapshotRepository.kt  the snapshots of each task (snapshots/<id>.jsonl)
    └── IsoDate.kt             ISO 8601 date text, used for the stored dates
```

## Navigation

`NavigationSuiteScaffold` shows the top-level screens (Home, Tasks, Settings) as a bottom bar in portrait and a navigation rail on wider windows such as landscape. Navigation Compose (`NavHost` in `AppNavigation.kt`, routes in `Routes.kt`) shows the screens. Home and Settings are nested graphs, so a tab stays selected while one of its inner pages is open, and each tab keeps its own back stack: leaving a tab and coming back returns to the page it was on.

- **Home** (`home/log`) shows one day's log under a row with the day. Tapping the row opens **Dates** (`home/dates`), a list of the days that have a log file, newest first, with today always listed. The day Home is showing has a tick. Tapping a day moves the tick to it and stays on the page; going back with the arrow shows that day's log on Home. The day is passed to Home through the previous entry's `savedStateHandle` (key `picked_day`, none means today). Swiping a day with a log to the left (`SwipeToDelete`) reveals a red Delete button, which deletes that day's file; if Home was showing that day, it goes back to today.
- **Settings** (`settings/list`) lists Server IP Addresses, Server Port and Name. The first row opens **Server IP Addresses** (`settings/server-ip-addresses`), a list of the phone's IPv4 addresses with their interface names.
- Screens change without an animation. Going back only acts while the page is resumed, so a fast double tap can't pop past the start screen and leave a blank page. Opening a page is single top, so a double tap opens it once.
- Every screen has its own `Scaffold` and top bar, and a list page is one `LazyColumn` of rows (`WListItem`, or `ListItem` for the text fields). The Dates and Server IP Addresses lists have dividers inset by 16 dp, and Home has one under the day row. Settings has none.

## Data flow

```
Start Scraper tapped    -> TaskScheduler.start() -> WorkManager (unique periodic work)
Start Web Server tapped -> ServerService.start() -> foreground service -> Ktor server on the saved port (8080 by default)
                                              |
                                  every ~15 min: TaskWorker.doWork() -> TaskRunner.runAll()

LogRepository.append(...) -> logs/yyyyMMdd.txt + StateFlow
                                              |
                              LandingScreen collects LogRepository.logs

TasksScreen collects TaskRepository.tasks (tasks.json)
```

- Button state is not held by the screen. `TaskScheduler.isRunning()` reads the job's status from WorkManager, and `ServerService.running` (kept in memory) says whether the web server is serving. The scraper and the web server are started and stopped separately from the menu on Home: each Start item is enabled only when that part isn't running, and each Stop item only when it is. The only thing that restarts the server is a changed Server Port in Settings, and only while it is running.
- Settings are saved automatically (no Save button) when the user presses Done or leaves a field. The saved name is not used by anything right now; it was sent in the removed dummy POST. The saved port is read by `ServerService` each time it starts.
- Tasks are not added in the Android UI. Start Web Server launches a web server on the phone for a desktop browser (see `features/web-server.md`); adding tasks through it comes next.
- There is no ViewModel yet. Add one per screen when a screen gains real logic.

## Backend

The app doesn't call a backend right now. The earlier dummy POST to `https://component.wanlok.workers.dev/api/dummy` (a Cloudflare Worker, repo `wanlok-component-worker`, which appends `{ name, datetime }` to the Firestore document `configs/dummy`) was removed. The plan is to save the text scraped by each task to Firestore the same way, through a new real endpoint.