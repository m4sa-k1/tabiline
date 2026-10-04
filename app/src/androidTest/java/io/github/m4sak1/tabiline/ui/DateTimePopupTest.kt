package io.github.m4sak1.tabiline.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.ui.components.AppDatePopup
import io.github.m4sak1.tabiline.ui.components.AppTimePopup
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DateTimePopupTest {
    @get:Rule val compose = createComposeRule()

    @Test fun calendarConfirmsOriginalDateWithoutTimezoneShift() {
        val original = LocalDate.of(2026, 10, 11)
        var result: LocalDate? = null
        var closed = false
        compose.setContent {
            TabilineTheme(ThemePreference.DARK, AccentPalette.GREEN) {
                AppDatePopup(original, { closed = true }, { result = it })
            }
        }
        compose.onNodeWithText("日付を選択").assertIsDisplayed()
        compose.onNodeWithText("決定").performClick()
        compose.waitUntil(5000) { closed }
        assertEquals(original, result)
    }

    @Test fun clockConfirmsExact24HourTime() {
        var result: LocalTime? = null
        var closed = false
        compose.setContent {
            TabilineTheme(ThemePreference.DARK, AccentPalette.GREEN) {
                AppTimePopup(LocalTime.of(23, 47), { closed = true }, { result = it })
            }
        }
        compose.onNodeWithText("時刻を選択").assertIsDisplayed()
        compose.onNodeWithText("決定").performClick()
        compose.waitUntil(5000) { closed }
        assertEquals(LocalTime.of(23, 47), result)
    }

    @Test fun clockCancelDoesNotCommit() {
        var committed = false
        var closed = false
        compose.setContent {
            TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
                AppTimePopup(LocalTime.NOON, { closed = true }, { committed = true })
            }
        }
        compose.onNodeWithText("キャンセル").performClick()
        compose.waitUntil(5000) { closed }
        assertFalse(committed)
    }
}
