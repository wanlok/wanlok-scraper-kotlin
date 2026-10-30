package com.wanlok.scraper.work

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wanlok.scraper.log.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PostWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            LogManager.append(applicationContext, "POST $ENDPOINT")
            val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.doOutput = true
                val body = JSONObject().put("name", Build.MODEL).toString()
                connection.outputStream.use { it.write(body.toByteArray()) }

                val code = connection.responseCode
                Log.d(TAG, "POST $ENDPOINT -> $code")
                LogManager.append(applicationContext, "-> $code")
                if (code in 200..299) Result.success() else Result.retry()
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "POST failed", e)
            LogManager.append(applicationContext, "error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PostWorker"
        private const val ENDPOINT = "https://component.wanlok.workers.dev/api/dummy"
        private const val TIMEOUT_MS = 15_000
    }
}