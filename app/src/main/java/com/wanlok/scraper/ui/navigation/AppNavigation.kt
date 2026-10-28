package com.wanlok.scraper.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.wanlok.scraper.ui.landing.LandingScreen
import com.wanlok.scraper.ui.settings.SettingsScreen

@Composable
fun AppNavigation() {
    var current by rememberSaveable { mutableStateOf(AppDestination.Landing) }

    // The default picks a bottom bar whenever the height is compact, which includes a phone in
    // landscape. Use the side rail there instead; otherwise keep the default choice.
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutType = if (isLandscape) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    selected = destination == current,
                    onClick = { current = destination },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val screenModifier = Modifier.padding(innerPadding)
            when (current) {
                AppDestination.Landing -> LandingScreen(modifier = screenModifier)
                AppDestination.Settings -> SettingsScreen(modifier = screenModifier)
            }
        }
    }
}