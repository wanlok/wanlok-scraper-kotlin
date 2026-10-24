package com.wanlok.scraper.ui.landing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wanlok.scraper.ui.theme.ScraperTheme
import com.wanlok.scraper.work.PostScheduler
import kotlinx.coroutines.flow.flowOf

@Composable
fun LandingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        // WorkManager isn't available in the @Preview renderer, so show "stopped" there.
        val isPreview = LocalInspectionMode.current
        val isRunningFlow = remember(context) {
            if (isPreview) flowOf(false) else PostScheduler.isRunning(context)
        }
        val isRunning by isRunningFlow.collectAsState(initial = false)

        Text(text = "Hello Android!")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { PostScheduler.start(context) },
                enabled = !isRunning
            ) {
                Text(text = "Start")
            }
            Button(
                onClick = { PostScheduler.stop(context) },
                enabled = isRunning
            ) {
                Text(text = "Stop")
            }
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