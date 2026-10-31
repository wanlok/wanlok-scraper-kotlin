package com.wanlok.scraper.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PostWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    // The dummy POST was removed. Running the saved tasks will go here.
    override suspend fun doWork(): Result = Result.success()
}