package io.github.m4sak1.tabiline.feature.editor

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class LegJsonEditorTest {
    @get:Rule val compose = createComposeRule()
    private val trip = Trip(id = 12, name = "東京旅行", startDate = LocalDate.of(2026, 11, 14), endDate = LocalDate.of(2026, 11, 15))
    private val flight = """{
        "mode":"FLIGHT","tripId":12,
        "departure":"2026-11-14T09:00:00+09:00","arrival":"2026-11-14T10:15:00+09:00",
        "departureZoneId":"Asia/Tokyo","arrivalZoneId":"Asia/Tokyo",
        "departurePlace":"伊丹","arrivalPlace":"羽田",
        "departureTerminal":"南","arrivalTerminal":"第2",
        "departurePlatform":"10","arrivalPlatform":"58",
        "boardingGroup":"2","flightNumber":"NH20","memo":"予約メモ","precedingGapType":"TRANSFER"
    }"""

    @Test fun importingFillsAllApplicableFieldsButRequiresExplicitSave() {
        var saved: TransportLeg? = null
        var standalone = true
        compose.setContent { TabilineTheme(ThemePreference.DARK, AccentPalette.GREEN) {
            LegEditorScreen(null, listOf(trip), null, null, "Asia/Tokyo", false, false, null,
                onBack = {}, onSave = { leg, unlinked -> saved = leg; standalone = unlinked }, onDelete = null)
        } }
        compose.onNodeWithContentDescription("追加メニューを開く").performClick()
        compose.onNodeWithText("AIに考えてもらう").assertIsEnabled()
        compose.onNodeWithText("JSONをコピペで読み込む").performClick()
        compose.onNodeWithText("JSONファイルを選ぶ").assertDoesNotExist()
        compose.onNodeWithText("JSONを貼り付け").performTextInput(flight)
        compose.onNodeWithText("入力欄へ反映").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("入力欄へ反映").fetchSemanticsNodes().isEmpty() }
        assertNull(saved)
        compose.onNodeWithText("NH20").performScrollTo().assertExists()
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.runOnIdle {
            val leg = requireNotNull(saved)
            assertFalse(standalone); assertEquals(12L, leg.tripId)
            assertEquals(TransportMode.FLIGHT, leg.mode)
            assertEquals(Instant.parse("2026-11-14T00:00:00Z"), leg.departure)
            assertEquals(Instant.parse("2026-11-14T01:15:00Z"), leg.arrival)
            assertEquals("Asia/Tokyo", leg.departureZoneId); assertEquals("Asia/Tokyo", leg.arrivalZoneId)
            assertEquals("伊丹", leg.departurePlace); assertEquals("羽田", leg.arrivalPlace)
            assertEquals("南", leg.departureTerminal); assertEquals("第2", leg.arrivalTerminal)
            assertEquals("10", leg.departurePlatform); assertEquals("58", leg.arrivalPlatform)
            assertEquals("2", leg.boardingGroup); assertEquals("NH20", leg.flightNumber)
            assertEquals("予約メモ", leg.memo); assertEquals(GapType.TRANSFER, leg.precedingGapType)
        }
    }

    @Test fun invalidJsonDoesNotReplaceDraft() {
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.PURPLE) {
            LegEditorScreen(null, emptyList(), null, null, "Asia/Tokyo", false, false, null,
                onBack = {}, onSave = { _, _ -> fail("Should not save") }, onDelete = null)
        } }
        compose.onNodeWithText("出発地").performScrollTo().performTextInput("入力済みの駅")
        compose.onNodeWithContentDescription("追加メニューを開く").performClick()
        compose.onNodeWithText("JSONをコピペで読み込む").performClick()
        compose.onNodeWithText("JSONを貼り付け").performTextInput("{}")
        compose.onNodeWithText("入力欄へ反映").performScrollTo().performClick()
        compose.onNodeWithText("JSONが壊れているか、必要な項目が不足しています。").assertExists()
        compose.onNodeWithContentDescription("JSONを閉じる").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("入力欄へ反映").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("入力済みの駅").assertExists()
    }

    @Test fun editScreenDoesNotShowImportMenu() {
        val existing = LegJsonCodec.decode(flight, listOf(trip), null).copy(id = 99)
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            LegEditorScreen(12, listOf(trip), existing, null, "Asia/Tokyo", false, false, null,
                onBack = {}, onSave = { _, _ -> }, onDelete = {})
        } }
        compose.onNodeWithContentDescription("追加メニューを開く").assertDoesNotExist()
    }
}
