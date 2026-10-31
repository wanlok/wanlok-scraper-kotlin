package com.wanlok.scraper.work

import android.content.Context
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import com.wanlok.scraper.data.Task
import com.wanlok.scraper.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.select.Selector
import java.io.IOException

/** Runs the saved tasks: fetch each page and log the text of the element its selector matches. */
object TaskRunner {
    private const val TIMEOUT_MS = 15_000
    private const val MAX_TEXT_LENGTH = 200

    /** Runs every saved task, one at a time. A task that fails doesn't stop the others. */
    suspend fun runAll(context: Context) {
        TaskRepository.load(context)
        TaskRepository.tasks.value.forEach { run(context, it) }
    }

    private suspend fun run(context: Context, task: Task) = withContext(Dispatchers.IO) {
        LogRepository.append(context, LogStatus.TASK_RUNNING, task.name)
        try {
            val document = Jsoup.connect(task.url).timeout(TIMEOUT_MS).get()
            val element = document.selectFirst(task.selector)
            if (element == null) {
                failed(context, task, "selector matched nothing")
            } else {
                val text = element.text().take(MAX_TEXT_LENGTH)
                LogRepository.append(context, LogStatus.TASK_COMPLETED, "${task.name}: $text")
            }
        } catch (e: HttpStatusException) {
            failed(context, task, "HTTP ${e.statusCode}")
        } catch (e: UnsupportedMimeTypeException) {
            failed(context, task, "not an HTML page")
        } catch (e: Selector.SelectorParseException) {
            failed(context, task, "invalid selector")
        } catch (e: IllegalArgumentException) {
            failed(context, task, "invalid URL")
        } catch (e: IOException) {
            failed(context, task, e.message ?: "network error")
        }
    }

    private fun failed(context: Context, task: Task, reason: String) {
        LogRepository.append(context, LogStatus.TASK_FAILED, "${task.name}: $reason")
    }
}
