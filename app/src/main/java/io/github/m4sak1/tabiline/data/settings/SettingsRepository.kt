package io.github.m4sak1.tabiline.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import io.github.m4sak1.tabiline.core.model.TransportMode
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.m4sak1.tabiline.core.model.ThemePreference
import io.github.m4sak1.tabiline.core.model.AccentPalette
import io.github.m4sak1.tabiline.core.model.UserSettings
import io.github.m4sak1.tabiline.core.model.FooterBlurMode
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
            footerBlurEnabled = prefs[booleanPreferencesKey("footer_blur_enabled")] ?: true,
            footerBlurMode = prefs[stringPreferencesKey("footer_blur_mode")]?.let {
                runCatching { FooterBlurMode.valueOf(it) }.getOrNull()
            } ?: FooterBlurMode.WITH_ADD_BUTTON,
            notificationsEnabled = prefs[booleanPreferencesKey("notifications_enabled")] ?: false,
            notificationMinutes = TransportMode.entries.associateWith {
                (prefs[intPreferencesKey("notify_${it.name}_minutes")] ?: if (it == TransportMode.FLIGHT) 60 else 10).coerceIn(0, 999)
            },
            theme = prefs[Keys.theme]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            accentPalette = prefs[Keys.accent]?.let { runCatching { AccentPalette.valueOf(it) }.getOrNull() }
                ?: AccentPalette.PURPLE,
            defaultZoneId = prefs[Keys.defaultZone] ?: "Asia/Tokyo",
            trainMinutes = prefs[Keys.train] ?: 10,
            busMinutes = prefs[Keys.bus] ?: 10,
            flightMinutes = prefs[Keys.flight] ?: 60,
            ferryMinutes = prefs[Keys.ferry] ?: 30,
            otherMinutes = prefs[Keys.other] ?: 15,
        )
    }

    override suspend fun update(settings: UserSettings) {
        context.settingsDataStore.edit {
            it[booleanPreferencesKey("footer_blur_enabled")] = settings.footerBlurEnabled
            it[stringPreferencesKey("footer_blur_mode")] = settings.footerBlurMode.name
            it[booleanPreferencesKey("notifications_enabled")] = settings.notificationsEnabled
            settings.notificationMinutes.forEach { (mode, minutes) -> it[intPreferencesKey("notify_${mode.name}_minutes")] = minutes.coerceIn(0, 999) }
            it[Keys.theme] = settings.theme.name
            it[Keys.accent] = settings.accentPalette.name
            it[Keys.defaultZone] = settings.defaultZoneId
            it[Keys.train] = settings.trainMinutes
            it[Keys.bus] = settings.busMinutes
            it[Keys.flight] = settings.flightMinutes
            it[Keys.ferry] = settings.ferryMinutes
            it[Keys.other] = settings.otherMinutes
        }
    }

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val accent = stringPreferencesKey("accent_palette")
        val defaultZone = stringPreferencesKey("default_zone_id")
        val train = intPreferencesKey("train_minutes")
        val bus = intPreferencesKey("bus_minutes")
        val flight = intPreferencesKey("flight_minutes")
        val ferry = intPreferencesKey("ferry_minutes")
        val other = intPreferencesKey("other_minutes")
    }
}
