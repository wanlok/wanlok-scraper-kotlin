package com.wanlok.scraper.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wanlok.scraper.ui.theme.ScraperTheme

@Composable
fun TasksScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Tasks")
    }
}

@Preview(showBackground = true)
@Composable
fun TasksScreenPreview() {
    ScraperTheme {
        TasksScreen()
    }
}