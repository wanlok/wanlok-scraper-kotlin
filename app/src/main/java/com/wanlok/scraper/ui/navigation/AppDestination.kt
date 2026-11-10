package com.wanlok.scraper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level screens shown in the bottom bar (portrait) or navigation rail (landscape). */
enum class AppDestination(val label: String, val icon: ImageVector, val route: String) {
    Home("Home", Icons.Filled.Home, Routes.HOME),
    Tasks("Tasks", Icons.Filled.Check, Routes.TASKS),
    Settings("Settings", Icons.Filled.Settings, Routes.SETTINGS)
}