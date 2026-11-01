package com.wanlok.scraper.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dates are stored as ISO 8601 text with the time zone, e.g. 2026-10-31T11:07:08.123+11:00. */
object IsoDate {
    private const val PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"

    fun format(date: Date): String = SimpleDateFormat(PATTERN, Locale.US).format(date)

    /** Throws [java.text.ParseException] if [text] isn't in that format. */
    fun parse(text: String): Date = SimpleDateFormat(PATTERN, Locale.US).parse(text)!!
}