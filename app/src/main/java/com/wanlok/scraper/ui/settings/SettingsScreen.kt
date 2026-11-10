package com.wanlok.scraper.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.wanlok.scraper.data.SettingsRepository
import com.wanlok.scraper.ui.theme.ScraperTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // DataStore isn't available in the @Preview renderer, so skip it there.
    val isPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        name = if (isPreview) Build.MODEL else SettingsRepository.name(context).first()
    }

    // Settings save automatically: when the user presses Done or leaves the field.
    fun saveName() {
        if (isPreview) return
        scope.launch {
            SettingsRepository.setName(context, name)
            // Show what was actually stored (a blank name goes back to the default).
            name = SettingsRepository.name(context).first()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { innerPadding ->
        // One list holds every setting: a row for each, with a divider between them.
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                ButtonRow(text = "Server IP Addresses", onClick = { })
                HorizontalDivider()
            }
            item {
                TextFieldRow(
                    label = "Name",
                    value = name,
                    onValueChange = { name = it },
                    onSave = ::saveName
                )
            }
        }
    }
}

/** A row that opens something when tapped, shown with an arrow on the right. */
@Composable
private fun ButtonRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable(onClick = onClick)
    )
}

/**
 * A row with the label on the left and an editable value on the right. It saves when the user
 * presses Done or leaves the field.
 */
@Composable
private fun TextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var wasFocused by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (wasFocused && !it.isFocused) onSave()
                        wasFocused = it.isFocused
                    }
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        // Tapping anywhere on the row edits the value, without the ripple.
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { focusRequester.requestFocus() }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ScraperTheme {
        SettingsScreen()
    }
}