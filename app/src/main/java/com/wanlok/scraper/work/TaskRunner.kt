package com.wanlok.scraper.work

import android.content.Context
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import com.wanlok.scraper.data.ScrapedItem
import com.wanlok.scraper.data.Snapshot
import com.wanlok.scraper.data.SnapshotRepository
import com.wanlok.scraper.data.Task
import com.wanlok.scraper.data.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs the saved tasks: find the pages of each task (its page selector, if it has one), load each
 * page in a WebView (see [PageScraper]), save one snapshot with what its data selector reads from
 * all the pages (or the error), and log the outcome.
 */
object TaskRunner {
    // Keeps the load on the site low when a task has several pages.
    private const val PAGE_PAUSE_MS = 1_000L

    // The ids of the tasks being run now. A task is never run twice at once, so two runs never save
    // snapshots of the same task together, but different tasks can run in parallel.
    private val running = ConcurrentHashMap.newKeySet<String>()

    // For runs that are started without waiting for them, see runOneInBackground.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Whether the task is being run now, by the periodic job or by [runOneInBackground]. */
    fun isRunning(taskId: String): Boolean = taskId in running

    /**
     * Runs every enabled task, one at a time. A task that is not enabled is logged and skipped, with
     * no snapshot, and so is a task that is already being run by [runOneInBackground]. A task that
     * fails doesn't stop the others.
     */
    suspend fun runAll(context: Context) {
        TaskRepository.load(context)
        TaskRepository.tasks.value.forEach { task ->
            if (running.add(task.id)) {
                runClaimed(context, task, enabled = task.enabled)
            } else {
                LogRepository.append(context, LogStatus.TASK_SKIPPED, "${task.name}: already running")
            }
        }
    }

    /**
     * Starts the task and returns at once. The outcome is in the log. Returns false, without
     * starting, if the task is already being run.
     */
    fun runOneInBackground(context: Context, task: Task): Boolean {
        if (!running.add(task.id)) return false
        scope.launch {
            try {
                // A run asked for by hand ignores the task's enabled flag.
                runClaimed(context, task, enabled = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nobody is waiting for this run, so an unexpected error is logged, not thrown.
                LogRepository.append(context, LogStatus.TASK_FAILED, "Run failed: ${e.message}")
            }
        }
        return true
    }

    /** Runs a task whose id is in [running], or logs it as skipped if [enabled] is false. Then lets go of it. */
    private suspend fun runClaimed(context: Context, task: Task, enabled: Boolean) {
        try {
            if (enabled) {
                run(context, task)
            } else {
                LogRepository.append(context, LogStatus.TASK_SKIPPED, task.name)
            }
        } finally {
            running.remove(task.id)
        }
    }

    private suspend fun run(context: Context, task: Task) {
        LogRepository.append(context, LogStatus.TASK_RUNNING, task.name)
        try {
            val data = scrapeAllPages(context, task)
            if (data.isEmpty()) {
                failed(context, task, "data selector matched nothing")
            } else {
                completed(context, task, data)
            }
        } catch (e: ScrapeException) {
            failed(context, task, e.message ?: "scrape failed")
        }
    }

    /**
     * The items of every page of the task, in page order. A task without a page selector has one
     * page, its URL. Otherwise the pages come from [PageScraper.pageUrls] and are read in turn,
     * with a short pause between them.
     */
    private suspend fun scrapeAllPages(context: Context, task: Task): List<ScrapedItem> {
        val urls = task.pageSelector?.let { PageScraper.pageUrls(context, task.url, it) }
            ?: listOf(task.url)

        // Progress is only logged for a task with several pages. One page needs no progress.
        val paged = task.pageSelector != null
        if (paged) {
            LogRepository.append(
                context, LogStatus.TASK_PAGES_FOUND, "${task.name}: ${count(urls.size, "page")}"
            )
        }

        val data = ArrayList<ScrapedItem>()
        for ((index, url) in urls.withIndex()) {
            if (index > 0) delay(PAGE_PAUSE_MS)
            val items = try {
                PageScraper.scrape(context, url, task.dataSelector)
            } catch (e: ScrapeException) {
                // With several pages, say which one failed.
                throw if (urls.size > 1) ScrapeException("page ${index + 1}: ${e.message}") else e
            }
            // A page with nothing on it ends the list. If it is the first, the task fails below.
            if (items.isEmpty()) break
            data += items
            if (paged) {
                LogRepository.append(
                    context,
                    LogStatus.TASK_PAGE_COMPLETED,
                    "${task.name}: page ${index + 1}/${urls.size}, ${count(items.size, "item")}"
                )
            }
        }
        return data
    }

    private fun count(number: Int, noun: String) = if (number == 1) "1 $noun" else "$number ${noun}s"

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