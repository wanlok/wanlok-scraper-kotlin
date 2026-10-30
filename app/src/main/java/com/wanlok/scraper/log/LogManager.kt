package com.wanlok.scraper.log

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lines shown on the log screen, which are today's lines. The worker runs in the background,
 * possibly while the UI isn't open, so lines are also saved to a file per day (logs/20261031.txt)
 * and today's file is reloaded the next time the app starts.
 */
object LogManager {
    private const val DIR_NAME = "logs"

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private var dir: File? = null
    private var day: String? = null

    @Synchronized
    fun load(context: Context) {
        if (dir != null) return
        dir = File(context.applicationContext.filesDir, DIR_NAME).apply { mkdirs() }
        switchTo(dayOf(Date()))
    }

    @Synchronized
    fun append(context: Context, message: String) {
        load(context)
        val now = Date()
        val today = dayOf(now)
        // The app can stay alive past midnight, so move to the new day's file when the date changes.
        if (today != day) switchTo(today)
        val line = "${SimpleDateFormat("HH:mm:ss", Locale.US).format(now)}  $message"
        _lines.value = _lines.value + line
        fileFor(today).appendText("$line\n")
    }

    private fun switchTo(newDay: String) {
        day = newDay
        val file = fileFor(newDay)
        _lines.value = if (file.exists()) file.readLines() else emptyList()
    }

    private fun fileFor(day: String) = File(dir, "$day.txt")

    private fun dayOf(date: Date) = SimpleDateFormat("yyyyMMdd", Locale.US).format(date)
}
