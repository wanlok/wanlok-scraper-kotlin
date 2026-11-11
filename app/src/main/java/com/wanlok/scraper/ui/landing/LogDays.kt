package com.wanlok.scraper.ui.landing

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Days are written yyyyMMdd, the same as the log file names, for example 20261108. */
object LogDays {
    private const val PATTERN = "yyyyMMdd"

    fun today(): String = SimpleDateFormat(PATTERN, Locale.US).format(Date())

    /** A day written the way the user's language does, for example 8 November 2026. */
    fun title(day: String): String {
        val date = try {
            SimpleDateFormat(PATTERN, Locale.US).parse(day)
        } catch (e: ParseException) {
            null
        } ?: return day
        return SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date)
    }
}