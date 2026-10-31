package com.wanlok.scraper.ui.landing

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
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.ui.icons.Stop
import com.wanlok.scraper.ui.theme.ScraperTheme
import com.wanlok.scraper.work.PostScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

private val sampleLines = listOf(
    "10:15:02  Started: POST every 15 minutes",
    "10:15:03  POST https://component.wanlok.workers.dev/api/dummy",
    "10:15:04  -> 200"
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
    val lines by (if (isPreview) remember { MutableStateFlow(sampleLines) } else LogRepository.lines)
        .collectAsState()

    LaunchedEffect(Unit) {
        if (!isPreview) withContext(Dispatchers.IO) { LogRepository.load(context) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(
                        onClick = { PostScheduler.start(context) },
                        enabled = !isRunning
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start")
                    }
                    IconButton(
                        onClick = { PostScheduler.stop(context) },
                        enabled = isRunning
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    }
                }
            )
        }
    ) { innerPadding ->
        LogViewer(
            lines = lines,
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