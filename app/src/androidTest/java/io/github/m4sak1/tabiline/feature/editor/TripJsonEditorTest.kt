package io.github.m4sak1.tabiline.feature.editor

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TripJsonEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun importPreviewRequiresSaveAndRegistersTheWholeTrip() {
        var saved: TripWithLegs? = null
        var id: Long? = null
        compose.setContent { TabilineTheme(ThemePreference.DARK, AccentPalette.GREEN) {
            TripEditorDialog(null, {}, { fail("Should save the entire plan") },
                onSavePlan = { saved = it; 42L }, onPlanSaved = { id = it })
        } }
        importJson(tripFixture)
        compose.onNodeWithText("取り込んだ予定 2件").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.waitUntil(5000) { id != null }
        compose.runOnIdle { assertEquals(42L, id); assertEquals(2, saved!!.legs.size); assertEquals("京都旅行", saved!!.trip.name) }
    }

    @Test fun failureKeepsImportedDraftForRetry() {
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            TripEditorDialog(null, {}, {}, onSavePlan = { error("保存テストエラー") })
        } }
        importJson(tripFixture)
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.onNodeWithText("保存テストエラー").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("京都旅行").assertExists()
        compose.onNodeWithText("取り込んだ予定 2件").assertExists()
    }

    @Test fun invalidJsonDoesNotChangeTripDraft() {
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) { TripEditorDialog(null, {}, {}) } }
        compose.onNodeWithText("旅行名").performTextInput("入力済み旅行")
        compose.onNodeWithContentDescription("追加メニューを開く").performClick()
        compose.onNodeWithText("JSONをコピペで読み込む").performClick()
        compose.onNodeWithText("JSONを貼り付け").performTextInput("{}")
        compose.onNodeWithText("入力欄へ反映").performScrollTo().performClick()
        compose.onNodeWithText("旅行全体のJSONが壊れているか、必須項目が不足しています。").assertExists()
        compose.onNodeWithContentDescription("JSONを閉じる").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("入力欄へ反映").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("入力済み旅行").assertExists()
    }

    @Test fun legEditorHasNoImportMenu() {
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            LegEditorScreen(null, emptyList(), null, null, "Asia/Tokyo", false, false, null, {}, { _, _ -> }, null)
        } }
        compose.onNodeWithContentDescription("追加メニューを開く").assertDoesNotExist()
    }

    private fun importJson(json: String) {
        compose.onNodeWithContentDescription("追加メニューを開く").performClick()
        compose.onNodeWithText("JSONをコピペで読み込む").performClick()
        compose.onNodeWithText("JSONファイルを選ぶ").assertDoesNotExist()
        compose.onNodeWithText("JSONを貼り付け").performTextInput(json)
        compose.onNodeWithText("入力欄へ反映").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("入力欄へ反映").fetchSemanticsNodes().isEmpty() }
    }
}
