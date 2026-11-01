package com.wanlok.scraper.work

import android.content.Context
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import com.wanlok.scraper.data.ScrapedItem
import com.wanlok.scraper.data.Snapshot
import com.wanlok.scraper.data.SnapshotRepository
import com.wanlok.scraper.data.Task
import com.wanlok.scraper.data.TaskRepository
import java.util.Date

/**
 * Runs the saved tasks: load each page in a WebView (see [PageScraper]), save a snapshot with the
 * text of every element its data selector matches (or the error), and log the outcome.
 */
object TaskRunner {
    /** Runs every saved task, one at a time. A task that fails doesn't stop the others. */
    suspend fun runAll(context: Context) {
        TaskRepository.load(context)
        TaskRepository.tasks.value.forEach { run(context, it) }
    }

    private suspend fun run(context: Context, task: Task) {
        LogRepository.append(context, LogStatus.TASK_RUNNING, task.name)
        try {
            val data = PageScraper.scrape(context, task.url, task.dataSelector)
            if (data.isEmpty()) {
                failed(context, task, "data selector matched nothing")
            } else {
                completed(context, task, data)
            }
        } catch (e: ScrapeException) {
            failed(context, task, e.message ?: "scrape failed")
        }
    }

    private fun completed(context: Context, task: Task, data: List<ScrapedItem>) {
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