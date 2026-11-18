package com.wanlok.scraper.data

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// There must be only one DataStore per file, so it's declared once at the top level.
private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * The app's saved settings. Both the Settings screen and the background worker read from here, so
 * a change takes effect on the next POST without restarting anything.
 */
object SettingsRepository {
    private val PORT = intPreferencesKey("port")
    private val NAME = stringPreferencesKey("name")

    private const val DEFAULT_PORT = 8080

    // An app cannot listen on the ports below 1024.
    private const val MIN_PORT = 1024

    /** The port the web server listens on. 8080 until the user sets one. */
    fun port(context: Context): Flow<Int> =
        context.applicationContext.dataStore.data.map { it[PORT] ?: DEFAULT_PORT }

    /**
     * Saves the port. Anything that is not a number from [MIN_PORT] to 65535 is ignored, since
     * the app cannot listen on the ports below that. Returns whether it was saved.
     */
    suspend fun setPort(context: Context, value: String): Boolean {
        val port = value.trim().toIntOrNull()?.takeIf { it in MIN_PORT..65535 } ?: return false
        context.applicationContext.dataStore.edit {
            if (port == DEFAULT_PORT) it.remove(PORT) else it[PORT] = port
        }
        return true
    }

    /** The name sent to the API. Falls back to the device model until the user sets one. */
    fun name(context: Context): Flow<String> =
        context.applicationContext.dataStore.data.map { it[NAME] ?: Build.MODEL }

    /** A blank name clears the setting, so [name] falls back to the default again. */
    suspend fun setName(context: Context, value: String) {
        val trimmed = value.trim()
        context.applicationContext.dataStore.edit {
            if (trimmed.isEmpty()) it.remove(NAME) else it[NAME] = trimmed
        }
    }
}