package com.wanlok.scraper.ui.landing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wanlok.scraper.ui.theme.ScraperTheme
import com.wanlok.scraper.work.PostScheduler

@Composable
fun LandingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        var isRunning by remember { mutableStateOf(false) }

        Text(text = "Hello Android!")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    PostScheduler.start(context)
                    isRunning = true
                },
                enabled = !isRunning
            ) {
                Text(text = "Start")
            }
            Button(
                onClick = {
                    PostScheduler.stop(context)
                    isRunning = false
                },
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