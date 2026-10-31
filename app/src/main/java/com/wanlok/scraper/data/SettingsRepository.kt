package com.wanlok.scraper.data

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
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
    private val NAME = stringPreferencesKey("name")

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