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
    val data: List<ScrapedItem>? = null,
    val error: String? = null
)

/** One item of a snapshot's data. It matches the task's [DataSelector]. */
sealed interface ScrapedItem {
    /** The text of one element (a task whose data selector is a string). */
    data class Text(val value: String) : ScrapedItem

    /** The fields read from one container, in the task's order. A field that wasn't found is null. */
    data class Fields(val values: Map<String, String?>) : ScrapedItem
}