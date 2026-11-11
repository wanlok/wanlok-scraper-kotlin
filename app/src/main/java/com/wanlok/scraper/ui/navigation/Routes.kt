package com.wanlok.scraper.ui.navigation

/** The address of every screen, used to navigate to it. */
object Routes {
    /** The Home graph, which holds the Home screens. Opening it shows its first screen. */
    const val HOME = "home"
    const val HOME_LOG = "home/log"
    const val DATES = "home/dates"

    const val TASKS = "tasks"

    /** The Settings graph, which holds the Settings screens. Opening it shows its first screen. */
    const val SETTINGS = "settings"
    const val SETTINGS_LIST = "settings/list"
    const val SERVER_IP_ADDRESSES = "settings/server-ip-addresses"
}