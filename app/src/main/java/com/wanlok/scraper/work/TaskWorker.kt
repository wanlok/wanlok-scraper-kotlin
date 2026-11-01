package com.wanlok.scraper.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** One run of the periodic job: runs all the saved tasks. */
class TaskWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    // Failures are logged per task, so the job itself always succeeds.
    override suspend fun doWork(): Result {
        TaskRunner.runAll(applicationContext)
        return Result.success()
    }
}