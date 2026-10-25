package com.wanlok.scraper.ui.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanlok.scraper.log.TerminalLog
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

@Composable
fun LandingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // WorkManager and the log file aren't available in the @Preview renderer, so fake them there.
    val isPreview = LocalInspectionMode.current
    val isRunningFlow = remember(context) {
        if (isPreview) flowOf(false) else PostScheduler.isRunning(context)
    }
    val isRunning by isRunningFlow.collectAsState(initial = false)
    val lines by (if (isPreview) remember { MutableStateFlow(sampleLines) } else TerminalLog.lines)
        .collectAsState()

    LaunchedEffect(Unit) {
        if (!isPreview) withContext(Dispatchers.IO) { TerminalLog.load(context) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Terminal(
            lines = lines,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { PostScheduler.start(context) },
                enabled = !isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Start")
            }
            Button(
                onClick = { PostScheduler.stop(context) },
                enabled = isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Stop")
            }
        }
    }
}

@Composable
private fun Terminal(lines: List<String>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    // Keep the newest line in view.
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        items(lines) { line ->
            Text(
                text = line,
                color = Color(0xFF3FB950),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
    ScraperTheme {
        LandingScreen()
    }
}