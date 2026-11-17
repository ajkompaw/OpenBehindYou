package dev.openbehindyou.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.openbehindyou.detector.Sensitivity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.guardSettings: DataStore<Preferences> by preferencesDataStore(
    name = "guard_settings",
)

/**
 * Only user preferences are persisted. The guard's running state is deliberately
 * not persisted: Android forbids starting a camera foreground service from the
 * background, so Guard must always be started by a visible user action.
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.guardSettings

    val sensitivity: Flow<Sensitivity> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[SENSITIVITY_KEY]
                ?.let { stored -> runCatching { Sensitivity.valueOf(stored) }.getOrNull() }
                ?: Sensitivity.MEDIUM
        }
        .distinctUntilChanged()

    suspend fun setSensitivity(sensitivity: Sensitivity) {
        dataStore.edit { preferences ->
            preferences[SENSITIVITY_KEY] = sensitivity.name
        }
    }

    private companion object {
        val SENSITIVITY_KEY = stringPreferencesKey("sensitivity")
    }
}
