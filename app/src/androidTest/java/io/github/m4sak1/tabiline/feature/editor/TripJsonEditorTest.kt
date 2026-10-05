package io.github.m4sak1.tabiline.feature.editor

import androidx.compose.ui.test.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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
        tripScreen(onSavePlan = { saved = it; 42L }, onPlanSaved = { id = it })
        importJson(tripFixture)
        compose.onNodeWithText("取り込んだ予定 2件").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.waitUntil(5000) { id != null }
        compose.runOnIdle { assertEquals(42L, id); assertEquals(2, saved!!.legs.size); assertEquals("京都旅行", saved!!.trip.name) }
    }

    @Test fun failureKeepsImportedDraftForRetry() {
        tripScreen(onSavePlan = { error("保存テストエラー") })
        importJson(tripFixture)
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.onNodeWithText("保存テストエラー").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("京都旅行").assertExists()
        compose.onNodeWithText("取り込んだ予定 2件").assertExists()
    }

    @Test fun invalidJsonDoesNotOpenTripEditor() {
        tripScreen(onSavePlan = { fail("No plan should be saved"); 0L })
        compose.onNodeWithContentDescription("追加メニューを開く").performClick()
        compose.onNodeWithText("JSONをコピペで読み込む").performClick()
        compose.onNodeWithText("JSONを貼り付け").performTextInput("{}")
        compose.onNodeWithText("入力欄へ反映").performScrollTo().performClick()
        compose.onNodeWithText("旅行全体のJSONが壊れているか、必須項目が不足しています。").assertExists()
        compose.onNodeWithContentDescription("JSONを閉じる").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("入力欄へ反映").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("新しい旅行").assertDoesNotExist()
        compose.onNodeWithContentDescription("追加メニューを開く").assertIsDisplayed()
    }

    @Test fun tripPopupContainsNoImportButton() {
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) { TripEditorDialog(null, {}, {}) } }
        compose.onNodeWithContentDescription("追加メニューを開く").assertDoesNotExist()
    }

    private fun tripScreen(onSavePlan: suspend (TripWithLegs) -> Long, onPlanSaved: (Long) -> Unit = {}) {
        var plan by mutableStateOf<TripWithLegs?>(null)
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            Box(Modifier.fillMaxSize()) {
                TripImportActions(enabled = plan == null, onPlanImported = { plan = it })
                plan?.let { TripEditorDialog(null, { plan = null }, {},
                    onSavePlan = onSavePlan, onPlanSaved = onPlanSaved, initialPlan = it) }
            }
        } }
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
