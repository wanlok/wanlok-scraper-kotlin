package com.wanlok.scraper.ui.navigation

import android.content.res.Configuration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.wanlok.scraper.ui.landing.LandingScreen
import com.wanlok.scraper.ui.settings.ServerIpAddressesScreen
import com.wanlok.scraper.ui.settings.SettingsScreen
import com.wanlok.scraper.ui.tasks.TasksScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

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
                    // A screen inside the Settings graph still counts as the Settings tab.
                    selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                    onClick = {
                        navController.navigate(destination.route) {
                            // Keep one copy of each tab, and bring back where it was left.
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) }
                )
            }
        }
    ) {
        // Each screen has its own Scaffold, so it can have its own top bar.
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) { LandingScreen() }
            composable(Routes.TASKS) { TasksScreen() }
            navigation(route = Routes.SETTINGS, startDestination = Routes.SETTINGS_LIST) {
                composable(Routes.SETTINGS_LIST) {
                    SettingsScreen(
                        onOpenServerIpAddresses = {
                            navController.navigate(Routes.SERVER_IP_ADDRESSES)
                        }
                    )
                }
                composable(Routes.SERVER_IP_ADDRESSES) {
                    ServerIpAddressesScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}