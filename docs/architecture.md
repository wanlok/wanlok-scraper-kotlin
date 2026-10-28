# Architecture

Single-Activity Jetpack Compose app (Material 3). Package: `com.wanlok.scraper`.

The app is at the proof-of-concept stage: the scheduled POST in `work/` is a placeholder for the real scraping job and will change.
SDK: `minSdk` 24, `targetSdk` 36, `compileSdk` 37.

## Layout

```
com.wanlok.scraper
├── MainActivity.kt        entry point: setContent { ScraperTheme { AppNavigation() } }
├── ui
│   ├── navigation/        AppNavigation.kt (NavigationSuiteScaffold), AppDestination.kt (top-level screens)
│   ├── landing/           LandingScreen.kt (terminal log + Start/Stop buttons)
│   ├── settings/          SettingsScreen.kt (placeholder)
│   └── theme/             Color.kt, Theme.kt, Type.kt
├── work
│   ├── PostScheduler.kt   starts/stops the periodic job, exposes its running state
│   └── PostWorker.kt      one run of the job: sends the POST
└── log
    └── TerminalLog.kt     lines shown on screen, persisted to a file
```

## Navigation

`NavigationSuiteScaffold` shows the top-level screens (Home, Settings) as a bottom bar in portrait and a navigation rail on wider windows such as landscape. The selected screen is held in `rememberSaveable` state, so it survives rotation. Navigation Compose is not used yet; add it when a screen needs drill-down destinations or a back stack.

## Data flow

```
Start tapped -> PostScheduler.start() -> WorkManager (unique periodic work)
                                              |
                                  every ~15 min: PostWorker.doWork()
                                              |
                              TerminalLog.append(...)  -> terminal.log + StateFlow
                                              |
                              LandingScreen collects TerminalLog.lines
```

- Button state is not held by the screen. `PostScheduler.isRunning()` reads the job's status from WorkManager, so the buttons are correct after the app is reopened.
- There is no ViewModel yet. Add one per screen when a screen gains real logic.

## Backend

`https://component.wanlok.workers.dev/api/dummy` is a Cloudflare Worker (repo: `wanlok-component-worker`).
`POST /api/dummy` expects JSON `{ "name": string }` and appends `{ name, datetime }` to the Firestore document `configs/dummy`.