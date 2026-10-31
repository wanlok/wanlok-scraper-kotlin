package com.wanlok.scraper.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Today's logs. The worker runs in the background, possibly while the UI isn't open, so
 * logs are also saved to a file per day (logs/20261031.txt), one JSON object per line, and
 * today's file is reloaded the next time the app starts.
 */
object LogRepository {
    private const val DIR_NAME = "logs"

    private val _logs = MutableStateFlow<List<WLog>>(emptyList())
    val logs: StateFlow<List<WLog>> = _logs.asStateFlow()

    private var dir: File? = null
    private var day: String? = null

    @Synchronized
    fun load(context: Context) {
        if (dir != null) return
        dir = File(context.applicationContext.filesDir, DIR_NAME).apply { mkdirs() }
        switchTo(dayOf(Date()))
    }

    @Synchronized
    fun append(context: Context, content: String) {
        load(context)
        val log = WLog(date = Date(), content = content)
        val today = dayOf(log.date)
        // The app can stay alive past midnight, so move to the new day's file when the date changes.
        if (today != day) switchTo(today)
        _logs.value = _logs.value + log
        fileFor(today).appendText(toJson(log) + "\n")
    }

    private fun switchTo(newDay: String) {
        day = newDay
        val file = fileFor(newDay)
        _logs.value = if (file.exists()) file.readLines().mapNotNull(::parse) else emptyList()
    }

    private fun toJson(log: WLog): String =
        JSONObject().put("date", isoFormat().format(log.date)).put("content", log.content).toString()

    // Skips lines that aren't valid logs, such as ones written in the old plain-text format.
    private fun parse(line: String): WLog? = try {
        val json = JSONObject(line)
        WLog(date = isoFormat().parse(json.getString("date"))!!, content = json.getString("content"))
    } catch (e: JSONException) {
        null
    } catch (e: ParseException) {
        null
    }

    // ISO 8601 with the time zone, e.g. 2026-10-31T11:07:08.123+11:00.
    private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)

    private fun fileFor(day: String) = File(dir, "$day.txt")

    private fun dayOf(date: Date) = SimpleDateFormat("yyyyMMdd", Locale.US).format(date)
}