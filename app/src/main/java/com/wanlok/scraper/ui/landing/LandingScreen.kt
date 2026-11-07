package com.wanlok.scraper.ui.landing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import com.wanlok.scraper.data.WLog
import com.wanlok.scraper.server.ServerService
import com.wanlok.scraper.ui.theme.ScraperTheme
import com.wanlok.scraper.work.TaskScheduler
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
        if (isPreview) flowOf(false) else TaskScheduler.isRunning(context)
    }
    val jobRunning by isRunningFlow.collectAsState(initial = false)
    // The job survives the app being swiped away, the web server doesn't, so they are shown apart.
    val serverRunning by (if (isPreview) flowOf(false) else ServerService.running)
        .collectAsState(initial = false)
    val logs by (if (isPreview) remember { MutableStateFlow(sampleLogs) } else LogRepository.logs)
        .collectAsState()

    LaunchedEffect(Unit) {
        if (!isPreview) withContext(Dispatchers.IO) { LogRepository.load(context) }
    }

    // Android 13+ asks before showing the server's notification. The server starts either way.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ServerService.start(context) }

    fun startWebServer() {
        val needsPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ServerService.start(context)
        }
    }

    // The actions live in an overflow menu, so more can be added without crowding the top bar.
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            MenuSubtitle("Web Server")
                            DropdownMenuItem(
                                text = { Text("Start") },
                                enabled = !serverRunning,
                                onClick = {
                                    menuOpen = false
                                    startWebServer()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Stop") },
                                enabled = serverRunning,
                                onClick = {
                                    menuOpen = false
                                    ServerService.stop(context)
                                }
                            )
                            MenuSubtitle("Scraper")
                            DropdownMenuItem(
                                text = { Text("Start") },
                                enabled = !jobRunning,
                                onClick = {
                                    menuOpen = false
                                    TaskScheduler.start(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Stop") },
                                enabled = jobRunning,
                                onClick = {
                                    menuOpen = false
                                    TaskScheduler.stop(context)
                                }
                            )
                        }
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

/** A small heading for a group of items in the menu. */
@Composable
private fun MenuSubtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
    ScraperTheme {
        LandingScreen()
    }
}