package com.wanlok.scraper.ui.tasks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wanlok.scraper.ui.theme.ScraperTheme

// Placeholder data until tasks come from a real source.
private val sampleTasks = List(20) { index ->
    Task(id = index, title = "Task ${index + 1}", description = "Placeholder description")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    tasks: List<Task> = sampleTasks
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Tasks") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // The key lets Compose keep each row's identity when the list changes.
            items(tasks, key = { it.id }) { task ->
                ListItem(
                    headlineContent = { Text(text = task.title) },
                    supportingContent = { Text(text = task.description) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TasksScreenPreview() {
    ScraperTheme {
        TasksScreen()
    }
}