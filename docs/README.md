# Docs

Specifications and design notes for this app. Keep them short and update them when behaviour changes.

- [architecture.md](architecture.md): how the code is organised and how data flows
- [features/background-post.md](features/background-post.md): the Start/Stop background POST job
- [features/web-server.md](features/web-server.md): the web server started by Start, for adding tasks from a desktop
- [adr/0001-workmanager-for-periodic-post.md](adr/0001-workmanager-for-periodic-post.md): why WorkManager, and its limits
- [adr/0002-foreground-service-and-ktor-for-web-server.md](adr/0002-foreground-service-and-ktor-for-web-server.md): why a foreground service and Ktor for the web server