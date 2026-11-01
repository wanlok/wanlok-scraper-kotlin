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

/**
 * Runs the saved tasks: find the pages of each task (its page selector, if it has one), load each
 * page in a WebView (see [PageScraper]), save one snapshot with what its data selector reads from
 * all the pages (or the error), and log the outcome.
 */
object TaskRunner {
    // Keeps the load on the site low when a task has several pages.
    private const val PAGE_PAUSE_MS = 1_000L

    /** Runs every saved task, one at a time. A task that fails doesn't stop the others. */
    suspend fun runAll(context: Context) {
        TaskRepository.load(context)
        TaskRepository.tasks.value.forEach { run(context, it) }
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