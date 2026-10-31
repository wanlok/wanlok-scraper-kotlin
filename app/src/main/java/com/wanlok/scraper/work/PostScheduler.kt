package com.wanlok.scraper.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wanlok.scraper.data.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

object PostScheduler {
    private const val WORK_NAME = "post_every_15_minutes"

    fun start(context: Context) {
        val request = PeriodicWorkRequestBuilder<PostWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // KEEP: tapping Start twice doesn't create a second job.
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        LogRepository.append(context, "Started")
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        LogRepository.append(context, "Stopped")
    }

    // Periodic work sits in ENQUEUED between runs; it only becomes finished (CANCELLED) after stop().
    fun isRunning(context: Context): Flow<Boolean> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(WORK_NAME)
            .map { infos -> infos.any { !it.state.isFinished } }
}
