package com.wanlok.scraper.data

/**
 * What a log records. [STARTED] and [STOPPED] are the user pressing Start and Stop. The TASK_
 * ones follow a task run: [TASK_RUNNING] while it works, then [TASK_COMPLETED] or [TASK_FAILED].
 */
enum class LogStatus { STARTED, STOPPED, TASK_RUNNING, TASK_COMPLETED, TASK_FAILED }