package com.wanlok.scraper.ui.landing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.ui.components.SwipeToDelete
import com.wanlok.scraper.ui.components.WListItem
import com.wanlok.scraper.ui.theme.ScraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val sampleDays = setOf("20261110", "20261109", "20261105")

/**
 * The days that have logs, newest first. Picking one shows that day's log on Home. The day
 * Home is showing now, [selectedDay], is highlighted. Swiping a day with logs to the left
 * reveals a Delete button, which deletes that day's log and then calls [onDeleted].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScreen(
    selectedDay: String,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    onDeleted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The log files aren't available in the @Preview renderer, so show a sample there.
    val isPreview = LocalInspectionMode.current

    // The days that have a log file. Null until they are read.
    var daysWithLogs by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(Unit) {
        daysWithLogs = if (isPreview) sampleDays else withContext(Dispatchers.IO) {
            LogRepository.daysWithLogs(context)
        }
    }
    // Today is always listed, so the live log can be reached before anything is logged.
    val days = daysWithLogs?.let { (it + LogDays.today()).sortedDescending() }.orEmpty()

    fun delete(day: String) {
        if (isPreview) return
        scope.launch {
            withContext(Dispatchers.IO) { LogRepository.delete(context, day) }
            daysWithLogs = withContext(Dispatchers.IO) { LogRepository.daysWithLogs(context) }
            onDeleted(day)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Dates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Keyed by day, so a row's swipe state doesn't move to the row that replaces it.
            items(days, key = { it }) { day ->
                val row = @Composable {
                    WListItem(
                        text = LogDays.title(day),
                        onClick = { onPick(day) },
                        selected = day == selectedDay
                    )
                }
                // A day without a log file, such as today before anything is logged, has
                // nothing to delete.
                if (day in daysWithLogs.orEmpty()) {
                    SwipeToDelete(onDelete = { delete(day) }) { row() }
                } else {
                    row()
                }
                HorizontalDivider()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DateScreenPreview() {
    ScraperTheme {
        DateScreen(selectedDay = "20261109", onBack = { }, onPick = { }, onDeleted = { })
    }
}