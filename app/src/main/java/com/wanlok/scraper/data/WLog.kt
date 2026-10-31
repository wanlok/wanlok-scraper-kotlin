package com.wanlok.scraper.data

import java.util.Date

/**
 * One log. [date] is when it happened. [content] is what happened: a task name, or an event
 * such as Started or Stopped.
 */
data class WLog(
    val date: Date,
    val content: String,
)