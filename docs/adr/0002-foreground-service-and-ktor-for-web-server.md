# 0002. Foreground service and Ktor (CIO) for the web server

Status: accepted

## Context
Tasks are added from a desktop browser, so the phone has to host a web server that stays reachable while the app is closed. WorkManager (ADR 0001) runs short jobs and cannot host a long-lived listener.

## Decision
- Run the server in a **foreground service** (`ServerService`) with a persistent notification.
- Use the `specialUse` foreground service type. `dataSync` is limited to about 6 hours per day on Android 15, which does not suit a server. `specialUse` is fine for a personal app that is not on Google Play.
- Use **Ktor server with the CIO engine** (3.x). It is Kotlin and coroutine based and actively maintained. NanoHTTPD was rejected: its last release was in 2016. The Netty engine was rejected: heavier on a phone and known for packaging problems on Android.
- Skip Ktor's JSON plugins; `org.json` is already used for the task file.

## Consequences
- A visible notification is shown while the server runs.
- Battery: a server that must answer with the screen off needs a wake lock and a Wi-Fi lock, which cost power. Planned as an opt-in setting.
- The periodic WorkManager job stays, and Start/Stop control both.
- Plain HTTP on the local network. Remote access later should go through a VPN such as Tailscale rather than port forwarding.