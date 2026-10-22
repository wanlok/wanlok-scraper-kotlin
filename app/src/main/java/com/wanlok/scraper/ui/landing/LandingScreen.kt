package com.wanlok.scraper.ui.landing

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wanlok.scraper.ui.theme.ScraperTheme

@Composable
fun LandingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Hello Android!")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { Log.d("LandingScreen", "Start clicked") }) {
                Text(text = "Start")
            }
            Button(onClick = { Log.d("LandingScreen", "Stop clicked") }) {
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