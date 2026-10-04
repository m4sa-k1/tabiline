package io.github.m4sak1.tabiline.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.m4sak1.tabiline.core.model.ThemePreference
import io.github.m4sak1.tabiline.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(settings: UserSettings)
}

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    override val settings = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            theme = prefs[Keys.theme]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            trainMinutes = prefs[Keys.train] ?: 10,
            busMinutes = prefs[Keys.bus] ?: 10,
            flightMinutes = prefs[Keys.flight] ?: 60,
            ferryMinutes = prefs[Keys.ferry] ?: 30,
            otherMinutes = prefs[Keys.other] ?: 15,
        )
    }

    override suspend fun update(settings: UserSettings) {
        context.settingsDataStore.edit {
            it[Keys.theme] = settings.theme.name
            it[Keys.train] = settings.trainMinutes
            it[Keys.bus] = settings.busMinutes
            it[Keys.flight] = settings.flightMinutes
            it[Keys.ferry] = settings.ferryMinutes
            it[Keys.other] = settings.otherMinutes
        }
    }

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val train = intPreferencesKey("train_minutes")
        val bus = intPreferencesKey("bus_minutes")
        val flight = intPreferencesKey("flight_minutes")
        val ferry = intPreferencesKey("ferry_minutes")
        val other = intPreferencesKey("other_minutes")
    }
}
