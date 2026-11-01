package com.wanlok.scraper.data

import java.util.Date

/**
 * What one run of a task produced, at [date]. [status] is [LogStatus.TASK_COMPLETED], with the
 * scraped [data] (a list: one item per matched element, later also e.g. image URLs), or
 * [LogStatus.TASK_FAILED], with the [error] that stopped it.
 */
data class Snapshot(
    val date: Date,
    val status: LogStatus,
    val data: List<String>? = null,
    val error: String? = null
)