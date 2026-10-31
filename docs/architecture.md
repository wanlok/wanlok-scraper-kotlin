# Architecture

Single-Activity Jetpack Compose app (Material 3). Package: `com.wanlok.scraper`.

The app is at an early stage: the periodic job in `work/` is a placeholder for running the saved tasks and will change.
SDK: `minSdk` 24, `targetSdk` 36, `compileSdk` 37.

## Layout

```
com.wanlok.scraper
├── MainActivity.kt        entry point: setContent { ScraperTheme { AppNavigation() } }
├── ui
│   ├── navigation/        AppNavigation.kt (NavigationSuiteScaffold), AppDestination.kt (top-level screens)
│   ├── landing/           LandingScreen.kt (log + Start/Stop icons in the top bar), LogViewer.kt
│   ├── tasks/             TasksScreen.kt (read-only list of the saved tasks)
│   ├── settings/          SettingsScreen.kt (scrollable list of settings, saved automatically)
│   ├── icons/             StopIcon.kt (Stop is only in material-icons-extended, so it is drawn here)
│   └── theme/             Color.kt, Theme.kt, Type.kt
├── work
│   ├── PostScheduler.kt   starts/stops the periodic job, exposes its running state
│   └── PostWorker.kt      one run of the job (placeholder, does nothing yet)
├── server
│   ├── ServerService.kt   foreground service that runs the Ktor web server (port 8080)
│   ├── TaskRoutes.kt      the /api/tasks endpoints (list, add, edit, delete)
│   └── NetworkAddress.kt  the phone's local IPv4 address
└── data
    ├── WLog.kt                one log: { date, status, content? }
    ├── LogStatus.kt           STARTED, STOPPED, TASK_RUNNING, TASK_COMPLETED or TASK_FAILED
    ├── LogRepository.kt       today's logs shown on screen, saved as JSON lines, one file per day
    ├── SettingsRepository.kt  saved settings (DataStore Preferences)
    ├── Task.kt                a scraping task: id, name, url, selector (text extraction only)
    └── TaskRepository.kt      the saved tasks (tasks.json)
```

## Navigation

`NavigationSuiteScaffold` shows the top-level screens (Home, Tasks, Settings) as a bottom bar in portrait and a navigation rail on wider windows such as landscape. The selected screen is held in `rememberSaveable` state, so it survives rotation. Navigation Compose is not used yet; add it when a screen needs drill-down destinations or a back stack.

## Data flow

```
Start tapped -> PostScheduler.start() -> WorkManager (unique periodic work)
             -> ServerService.start() -> foreground service -> Ktor server on port 8080
                                              |
                                  every ~15 min: PostWorker.doWork()  (placeholder)

LogRepository.append(...) -> logs/yyyyMMdd.txt + StateFlow
                                              |
                              LandingScreen collects LogRepository.logs

TasksScreen collects TaskRepository.tasks (tasks.json)
```

- Button state is not held by the screen. `PostScheduler.isRunning()` reads the job's status from WorkManager, so the icons are correct after the app is reopened.
- Settings are saved automatically (no Save button) when the user presses Done or leaves a field. The saved name is not used by anything right now; it was sent in the removed dummy POST.
- Tasks are not added in the Android UI. Start launches a web server on the phone for a desktop browser (see `features/web-server.md`); adding tasks through it comes next.
- There is no ViewModel yet. Add one per screen when a screen gains real logic.

## Backend

The app doesn't call a backend right now. The earlier dummy POST to `https://component.wanlok.workers.dev/api/dummy` (a Cloudflare Worker, repo `wanlok-component-worker`, which appends `{ name, datetime }` to the Firestore document `configs/dummy`) was removed. The plan is to save the text scraped by each task to Firestore the same way, through a new real endpoint.