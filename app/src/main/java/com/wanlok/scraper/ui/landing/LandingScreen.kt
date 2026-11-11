package com.wanlok.scraper.ui.landing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wanlok.scraper.data.LogRepository
import com.wanlok.scraper.data.LogStatus
import com.wanlok.scraper.data.WLog
import com.wanlok.scraper.server.ServerService
import com.wanlok.scraper.ui.components.WListItem
import com.wanlok.scraper.ui.theme.ScraperTheme
import com.wanlok.scraper.work.TaskScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val now = System.currentTimeMillis()

private val sampleLogs = listOf(
    WLog(date = Date(now - 3_000), status = LogStatus.STARTED),
    WLog(date = Date(now - 2_000), status = LogStatus.TASK_RUNNING, content = "Task 1"),
    WLog(date = Date(now - 1_000), status = LogStatus.TASK_COMPLETED, content = "Task 1"),
    WLog(date = Date(now), status = LogStatus.TASK_FAILED, content = "Task 2")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // WorkManager and the log file aren't available in the @Preview renderer, so fake them there.
    val isPreview = LocalInspectionMode.current
    val isRunningFlow = remember(context) {
        if (isPreview) flowOf(false) else TaskScheduler.isRunning(context)
    }
    val jobRunning by isRunningFlow.collectAsState(initial = false)
    // The job survives the app being swiped away, the web server doesn't, so they are shown apart.
    val serverRunning by (if (isPreview) flowOf(false) else ServerService.running)
        .collectAsState(initial = false)
    val logs by (if (isPreview) remember { MutableStateFlow(sampleLogs) } else LogRepository.logs)
        .collectAsState()

    LaunchedEffect(Unit) {
        if (!isPreview) withContext(Dispatchers.IO) { LogRepository.load(context) }
    }

    // Android 13+ asks before showing the server's notification. The server starts either way.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ServerService.start(context) }

    fun startWebServer() {
        val needsPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ServerService.start(context)
        }
    }

    // The actions live in an overflow menu, so more can be added without crowding the top bar.
    var menuOpen by remember { mutableStateOf(false) }

    // The day being viewed. The date picker gives midnight UTC of the picked day; null means today.
    var pickerOpen by remember { mutableStateOf(false) }
    var pickedMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val todayDay = remember { SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) }
    val pickedDay = pickedMillis?.let { utcFormat("yyyyMMdd", it) }
    val viewingToday = pickedDay == null || pickedDay == todayDay

    // Written the way the user's language does, for example 8 November 2026.
    val dateText = pickedMillis?.let { utcFormat("d MMMM yyyy", it, Locale.getDefault()) }
        ?: remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date()) }

    // Today's logs are live; a past day is read once from its file.
    var pastLogs by remember { mutableStateOf<List<WLog>>(emptyList()) }
    LaunchedEffect(pickedDay) {
        pastLogs = if (viewingToday || pickedDay == null) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) { LogRepository.read(context, pickedDay) }
        }
    }

    // The picker decides which days are enabled when it draws them, so it must not open
    // before the list of days is ready. It is read again each time the picker is opened.
    val loadedDays by produceState<Set<String>?>(initialValue = null, pickerOpen) {
        value = null
        if (pickerOpen) {
            value = if (isPreview) emptySet() else withContext(Dispatchers.IO) {
                LogRepository.daysWithLogs(context)
            }
        }
    }
    val daysWithLogs = loadedDays

    if (pickerOpen && daysWithLogs != null) {
        // Only days with logs can be picked, plus today so the live log is always reachable.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = pickedMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = utcFormat("yyyyMMdd", utcTimeMillis)
                    return day == todayDay || day in daysWithLogs
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            shape = RectangleShape,
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickedMillis = pickerState.selectedDateMillis
                        pickerOpen = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickerOpen = false }) { Text("Cancel") }
            }
        ) {
            // Only the calendar: no "Select date" title, no headline and no keyboard-input toggle.
            DatePicker(
                state = pickerState,
                title = null,
                headline = null,
                showModeToggle = false
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            MenuSubtitle("Web Server")
                            DropdownMenuItem(
                                text = { Text("Start") },
                                enabled = !serverRunning,
                                onClick = {
                                    menuOpen = false
                                    startWebServer()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Stop") },
                                enabled = serverRunning,
                                onClick = {
                                    menuOpen = false
                                    ServerService.stop(context)
                                }
                            )
                            MenuSubtitle("Scraper")
                            DropdownMenuItem(
                                text = { Text("Start") },
                                enabled = !jobRunning,
                                onClick = {
                                    menuOpen = false
                                    TaskScheduler.start(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Stop") },
                                enabled = jobRunning,
                                onClick = {
                                    menuOpen = false
                                    TaskScheduler.stop(context)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // The day whose log is shown. Tap it to pick another day.
            WListItem(
                text = dateText,
                onClick = { pickerOpen = true },
                icon = Icons.Filled.KeyboardArrowDown
            )
            LogViewer(
                logs = if (viewingToday) logs else pastLogs,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

/** Formats a date picker value, which is midnight UTC, without shifting it to another day. */
private fun utcFormat(pattern: String, millis: Long, locale: Locale = Locale.US): String =
    SimpleDateFormat(pattern, locale).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(millis))

/** A small heading for a group of items in the menu. */

@Composable
private fun MenuSubtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
    ScraperTheme {
        LandingScreen()
    }
}