package com.wanlok.scraper.ui.landing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wanlok.scraper.data.WLog
import com.wanlok.scraper.ui.theme.ScraperTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogViewer(logs: List<WLog>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    // Keep the newest log in view.
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.scrollToItem(logs.lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(logs) { log ->
            Text(text = "${timeFormat.format(log.date)}  ${log.content}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LogViewerPreview() {
    val now = System.currentTimeMillis()
    ScraperTheme {
        LogViewer(
            logs = listOf(
                WLog(date = Date(now - 2_000), content = "Started"),
                WLog(
                    date = Date(now - 1_000),
                    content = "A task with a long name that wraps onto a second line"
                ),
                WLog(date = Date(now), content = "Task 2")
            )
        )
    }
}