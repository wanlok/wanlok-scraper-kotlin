package com.wanlok.scraper.ui.landing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import com.wanlok.scraper.data.WLog
import com.wanlok.scraper.server.ServerService
import com.wanlok.scraper.ui.icons.Stop
import com.wanlok.scraper.ui.theme.ScraperTheme
import com.wanlok.scraper.work.PostScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.util.Date

private val now = System.currentTimeMillis()

private val sampleLogs = listOf(
    WLog(date = Date(now - 3_000), status = LogStatus.STARTED),
    WLog(date = Date(now - 2_000), status = LogStatus.TASK_RUNNING, content = "Task 1"),
    WLog(date = Date(now - 1_000), status = LogStatus.TASK_COMPLETED, content = "Task 1"),
    WLog(date = Date(now), status = LogStatus.TASK_FAILED, content = "Task 2")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // WorkManager and the log file aren't available in the @Preview renderer, so fake them there.
    val isPreview = LocalInspectionMode.current
    val isRunningFlow = remember(context) {
        if (isPreview) flowOf(false) else PostScheduler.isRunning(context)
    }
    val isRunning by isRunningFlow.collectAsState(initial = false)
    val logs by (if (isPreview) remember { MutableStateFlow(sampleLogs) } else LogRepository.logs)
        .collectAsState()

    LaunchedEffect(Unit) {
        if (!isPreview) withContext(Dispatchers.IO) { LogRepository.load(context) }
    }

    // Start runs the periodic job and the web server together.
    fun start() {
        PostScheduler.start(context)
        ServerService.start(context)
    }
    // Android 13+ asks before showing the server's notification. Start either way.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { start() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(
                        onClick = {
                            val needsPermission =
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                start()
                            }
                        },
                        enabled = !isRunning
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start")
                    }
                    IconButton(
                        onClick = {
                            PostScheduler.stop(context)
                            ServerService.stop(context)
                        },
                        enabled = isRunning
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    }
                }
            )
        }
    ) { innerPadding ->
        LogViewer(
            logs = logs,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
    ScraperTheme {
        LandingScreen()
    }
}