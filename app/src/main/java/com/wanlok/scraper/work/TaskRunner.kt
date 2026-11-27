package com.wanlok.scraper.work

import android.content.Context
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import com.wanlok.scraper.data.ScrapedItem
import com.wanlok.scraper.data.Snapshot
import com.wanlok.scraper.data.SnapshotRepository
import com.wanlok.scraper.data.Task
import com.wanlok.scraper.data.TaskRepository
import kotlinx.coroutines.delay
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

    // The ids of the saved tasks being run by the periodic job now, so it never runs one twice.
    private val running = ConcurrentHashMap.newKeySet<String>()

    /** Whether the periodic job is running the task now. */
    fun isRunning(taskId: String): Boolean = taskId in running

    /**
     * Runs every scheduled task, one at a time. A task that is not scheduled is logged and skipped, with
     * no snapshot, and so is a task that is already being run. A task that fails doesn't stop the others.
     */
    suspend fun runAll(context: Context) {
        TaskRepository.load(context)
        TaskRepository.tasks.value.forEach { task ->
            if (running.add(task.id)) {
                runClaimed(context, task, scheduled = task.scheduled)
            } else {
                LogRepository.append(context, LogStatus.TASK_SKIPPED, "${task.name}: already running")
            }
        }
    }

    /**
     * Runs a saved task, whatever its scheduled flag, saves the snapshot and returns it. Returns
     * null, without running, if the task is already being run. The task's id says where the
     * snapshot goes, and its other fields are what is run, so they need not match the saved task.
     */
    suspend fun runSaved(context: Context, task: Task): Snapshot? {
        if (!running.add(task.id)) return null
        try {
            return runUnsaved(context, task).also { SnapshotRepository.append(context, task.id, it) }
        } finally {
            running.remove(task.id)
        }
    }

    /**
     * Runs a task that is not saved, whatever its scheduled flag, and returns what it produced: a
     * completed snapshot or a failed one. Nothing is stored. The outcome is logged like any run.
     */
    suspend fun runUnsaved(context: Context, task: Task): Snapshot {
        LogRepository.append(context, LogStatus.TASK_RUNNING, task.name)
        val snapshot = try {
            val data = scrapeAllPages(context, task)
            if (data.isEmpty()) {
                Snapshot(Date(), LogStatus.TASK_FAILED, error = "data selector matched nothing")
            } else {
                Snapshot(Date(), LogStatus.TASK_COMPLETED, data = data)
            }
        } catch (e: ScrapeException) {
            Snapshot(Date(), LogStatus.TASK_FAILED, error = e.message ?: "scrape failed")
        }
        if (snapshot.status == LogStatus.TASK_COMPLETED) {
            LogRepository.append(context, LogStatus.TASK_COMPLETED, task.name)
        } else {
            LogRepository.append(context, LogStatus.TASK_FAILED, "${task.name}: ${snapshot.error}")
        }
        return snapshot
    }

    /** Runs a task whose id is in [running], or logs it as skipped if [scheduled] is false. Then lets go of it. */
    private suspend fun runClaimed(context: Context, task: Task, scheduled: Boolean) {
        try {
            if (scheduled) {
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