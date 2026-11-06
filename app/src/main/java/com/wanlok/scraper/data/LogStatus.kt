package com.wanlok.scraper.data

/**
 * What a log records. [STARTED] and [STOPPED] are the user pressing Start and Stop. The TASK_
 * ones follow a task run: [TASK_RUNNING] while it works, then [TASK_COMPLETED] or [TASK_FAILED].
 * A task with several pages also logs [TASK_PAGES_FOUND] once, when it knows how many pages there
 * are, and [TASK_PAGE_COMPLETED] after each page. A task that is not enabled logs [TASK_SKIPPED]
 * instead of running.
 */
enum class LogStatus {
    STARTED,
    STOPPED,
    TASK_RUNNING,
    TASK_PAGES_FOUND,
    TASK_PAGE_COMPLETED,
    TASK_COMPLETED,
    TASK_FAILED,
    TASK_SKIPPED
}