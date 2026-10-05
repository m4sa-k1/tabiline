package io.github.m4sak1.tabiline.feature.settings

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FooterBlurSettingsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun togglesBlurWithoutChangingOtherPreferences() {
        var settings by mutableStateOf(UserSettings(defaultZoneId = "Europe/London"))
        compose.setContent { TabilineTheme(ThemePreference.DARK, AccentPalette.GREEN) {
            SettingsDetailPopup(SettingsSection.DISPLAY, settings, { settings = it }, {}, {})
        } }
        compose.onNodeWithContentDescription("フッター背景ぼかし").assertIsOn().performClick().assertIsOff()
        compose.runOnIdle { assertFalse(settings.footerBlurEnabled); assertEquals("Europe/London", settings.defaultZoneId) }
        compose.onNodeWithContentDescription("フッター背景ぼかし").performClick().assertIsOn()
        compose.runOnIdle { assertTrue(settings.footerBlurEnabled) }
    }
}
