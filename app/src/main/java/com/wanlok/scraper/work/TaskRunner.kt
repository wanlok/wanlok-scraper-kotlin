package com.wanlok.scraper.work

import android.content.Context
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import com.wanlok.scraper.data.Snapshot
import com.wanlok.scraper.data.SnapshotRepository
import com.wanlok.scraper.data.Task
import com.wanlok.scraper.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.select.Selector
import java.io.IOException
import java.util.Date

/**
 * Runs the saved tasks: fetch each page, save a snapshot with the text of the element its data selector
 * matches (or the error), and log the outcome.
 */
object TaskRunner {
    private const val TIMEOUT_MS = 15_000

    /** Runs every saved task, one at a time. A task that fails doesn't stop the others. */
    suspend fun runAll(context: Context) {
        TaskRepository.load(context)
        TaskRepository.tasks.value.forEach { run(context, it) }
    }

    private suspend fun run(context: Context, task: Task) = withContext(Dispatchers.IO) {
        LogRepository.append(context, LogStatus.TASK_RUNNING, task.name)
        try {
            val document = Jsoup.connect(task.url).timeout(TIMEOUT_MS).get()
            val element = document.selectFirst(task.dataSelector)
            if (element == null) {
                failed(context, task, "data selector matched nothing")
            } else {
                completed(context, task, element.text())
            }
        } catch (e: HttpStatusException) {
            failed(context, task, "HTTP ${e.statusCode}")
        } catch (e: UnsupportedMimeTypeException) {
            failed(context, task, "not an HTML page")
        } catch (e: Selector.SelectorParseException) {
            failed(context, task, "invalid data selector")
        } catch (e: IllegalArgumentException) {
            failed(context, task, "invalid URL")
        } catch (e: IOException) {
            failed(context, task, e.message ?: "network error")
        }
    }

    private fun completed(context: Context, task: Task, data: String) {
        val snapshot = Snapshot(Date(), LogStatus.TASK_COMPLETED, data = data)
        SnapshotRepository.append(context, task.id, snapshot)
        LogRepository.append(context, LogStatus.TASK_COMPLETED, task.name)
    }

    private fun failed(context: Context, task: Task, reason: String) {
        val snapshot = Snapshot(Date(), LogStatus.TASK_FAILED, error = reason)
        SnapshotRepository.append(context, task.id, snapshot)
        LogRepository.append(context, LogStatus.TASK_FAILED, "${task.name}: $reason")
    }
}