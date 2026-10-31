package com.wanlok.scraper.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PostWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            LogRepository.append(applicationContext, "POST $ENDPOINT")
            val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.doOutput = true
                val name = SettingsRepository.name(applicationContext).first()
                val body = JSONObject().put("name", name).toString()
                connection.outputStream.use { it.write(body.toByteArray()) }

                val code = connection.responseCode
                Log.d(TAG, "POST $ENDPOINT -> $code")
                LogRepository.append(applicationContext, "-> $code")
                if (code in 200..299) Result.success() else Result.retry()
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "POST failed", e)
            LogRepository.append(applicationContext, "error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PostWorker"
        private const val ENDPOINT = "https://component.wanlok.workers.dev/api/dummy"
        private const val TIMEOUT_MS = 15_000
    }
}