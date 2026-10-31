package com.wanlok.scraper.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.wanlok.scraper.data.Task
import com.wanlok.scraper.data.TaskRepository
import com.wanlok.scraper.ui.theme.ScraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

// Shown in the @Preview, where the saved tasks aren't available.
private val sampleTasks = List(20) { index ->
    Task(
        id = index.toString(),
        name = "Task ${index + 1}",
        url = "https://example.com/page-${index + 1}",
        selector = "h1.title"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val tasks by (if (isPreview) remember { MutableStateFlow(sampleTasks) } else TaskRepository.tasks)
        .collectAsState()

    LaunchedEffect(Unit) {
        if (!isPreview) withContext(Dispatchers.IO) { TaskRepository.load(context) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Tasks") }) }
    ) { innerPadding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No tasks")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // The key lets Compose keep each row's identity when the list changes.
                items(tasks, key = { it.id }) { task ->
                    ListItem(
                        headlineContent = { Text(text = task.name) },
                        supportingContent = {
                            Column {
                                Text(text = task.url)
                                Text(text = task.selector)
                            }
                        }
                    )
                    HorizontalDivider()
                }
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