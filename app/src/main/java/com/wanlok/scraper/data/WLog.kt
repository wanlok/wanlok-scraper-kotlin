package com.wanlok.scraper.data

import java.util.Date

/**
 * One log. [date] is when it happened and [status] is what happened. [content] is the detail,
 * such as a task name; it is null when there is nothing to add (STARTED, STOPPED).
 */
data class WLog(
    val date: Date,
    val status: LogStatus,
    val content: String? = null,
)