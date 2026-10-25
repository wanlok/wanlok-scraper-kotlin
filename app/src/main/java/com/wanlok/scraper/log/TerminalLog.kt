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
 * Lines shown on the terminal-style screen. The worker runs in the background, possibly while the
 * UI isn't open, so lines are also saved to a file and reloaded the next time the app starts.
 */
object TerminalLog {
    private const val FILE_NAME = "terminal.log"
    private const val MAX_LINES = 500

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private var file: File? = null

    @Synchronized
    fun load(context: Context) {
        if (file != null) return
        val logFile = File(context.applicationContext.filesDir, FILE_NAME)
        file = logFile
        if (logFile.exists()) {
            _lines.value = logFile.readLines().takeLast(MAX_LINES)
        }
    }

    @Synchronized
    fun append(context: Context, message: String) {
        load(context)
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val updated = (_lines.value + "$time  $message").takeLast(MAX_LINES)
        _lines.value = updated
        file?.writeText(updated.joinToString("\n", postfix = "\n"))
    }
}