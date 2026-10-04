package io.github.m4sak1.tabiline.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.feature.editor.LegEditorScreen
import io.github.m4sak1.tabiline.feature.editor.TripEditorDialog
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EditorSafetyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tripCancelConfirmsOnlyAfterAnEdit() {
        var closed = false
        compose.setContent {
            TabilineTheme(ThemePreference.DARK, AccentPalette.GREEN) {
                TripEditorDialog(null, { closed = true }, {})
            }
        }
        compose.onNodeWithText("旅行名").performTextInput("テスト旅行")
        compose.onNodeWithText("キャンセル").performClick()
        compose.onNodeWithText("入力内容を破棄しますか？").assertIsDisplayed()
        assertFalse(closed)
        compose.onNodeWithText("入力を続ける").performClick()
        compose.onNodeWithText("テスト旅行").assertExists()
        compose.onNodeWithContentDescription("閉じる").performClick()
        compose.onNodeWithText("破棄して閉じる").performClick()
        compose.waitUntil(5000) { closed }
    }

    @Test fun movementCloseKeepsDraftUntilConfirmed() {
        var closed = false
        compose.setContent {
            TabilineTheme(ThemePreference.DARK, AccentPalette.PURPLE) {
                LegEditorScreen(null, emptyList(), null, null, "Asia/Tokyo", false, false, null,
                    onBack = { closed = true }, onSave = { _, _ -> }, onDelete = null)
            }
        }
        compose.onNodeWithText("出発地").performScrollTo().performTextInput("高松")
        compose.onNodeWithContentDescription("閉じる").performScrollTo().performClick()
        compose.onNodeWithText("入力内容を破棄しますか？").assertIsDisplayed()
        assertFalse(closed)
        compose.onNodeWithText("入力を続ける").performClick()
        compose.onNodeWithContentDescription("閉じる").performClick()
        compose.onNodeWithText("破棄して閉じる").performClick()
        compose.runOnIdle { assertTrue(closed) }
    }
}
