package com.wanlok.scraper.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/** Starts and stops the periodic job that runs the saved tasks (see [TaskWorker]). */
object TaskScheduler {
    private const val WORK_NAME = "run_tasks_every_15_minutes"

    fun start(context: Context) {
        val request = PeriodicWorkRequestBuilder<TaskWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // KEEP: tapping Start twice doesn't create a second job.
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        LogRepository.append(context, LogStatus.STARTED, "Scraper")
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        LogRepository.append(context, LogStatus.STOPPED, "Scraper")
    }

    // Periodic work sits in ENQUEUED between runs; it only becomes finished (CANCELLED) after stop().
    fun isRunning(context: Context): Flow<Boolean> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(WORK_NAME)
            .map { infos -> infos.any { !it.state.isFinished } }
}
