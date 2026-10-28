package com.wanlok.scraper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Top-level screens shown in the bottom bar (portrait) or navigation rail (landscape). */
enum class AppDestination(val label: String, val icon: ImageVector) {
    Landing("Home", Icons.Filled.Home),
    Settings("Settings", Icons.Filled.Settings)
}