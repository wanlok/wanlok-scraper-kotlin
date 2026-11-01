# 0001: Use WorkManager for the periodic POST

Status: accepted for the proof of concept (the periodic POST is a placeholder for the real job, so revisit this when that job is defined)

## Context
The app must run work on a schedule while the app is in the background or closed. For the proof of concept that work is a single dummy POST. The requested interval was first 5 minutes, then relaxed to 15 minutes.

## Options
- **WorkManager periodic work**: minimum interval 15 minutes, survives app restarts and reboots, battery-friendly, no notification.
- **Foreground service with a loop**: can run every 5 minutes, but needs a persistent notification, a foreground service type, and extra permissions. Android 15 limits some types.
- **AlarmManager**: timing drifts under Doze, so no better than WorkManager for this.

## Decision
WorkManager, with a 15-minute periodic request, a network constraint, and unique work with `ExistingPeriodicWorkPolicy.KEEP`.

## Consequences
- Runs are approximate and can be delayed by the system.
- If a shorter interval is ever needed, this must be replaced with a foreground service.
- Button state is derived from WorkManager's status, not stored in the UI.