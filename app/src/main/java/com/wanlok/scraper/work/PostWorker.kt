package com.wanlok.scraper.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PostWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    // Failures are logged per task, so the job itself always succeeds.
    override suspend fun doWork(): Result {
        TaskRunner.runAll(applicationContext)
        return Result.success()
    }
}