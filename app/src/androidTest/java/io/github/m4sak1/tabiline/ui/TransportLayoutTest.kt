package io.github.m4sak1.tabiline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.*
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.feature.editor.TripEditorDialog
import io.github.m4sak1.tabiline.feature.editor.TripImportActions
import io.github.m4sak1.tabiline.feature.editor.LegEditorScreen
import io.github.m4sak1.tabiline.feature.timeline.TimelineScreen
import io.github.m4sak1.tabiline.feature.home.HomeScreen
import io.github.m4sak1.tabiline.ui.components.*
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TransportLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.now().plusDays(1)
    private val start = day.atTime(19, 41).atZone(ZoneId.of("Asia/Tokyo")).toInstant()
    private val trip = Trip(1, "テスト旅行", day, day.plusDays(2))
    private val leg = TransportLeg(id = 1, tripId = 1, departure = start,
        arrival = start.plusSeconds(36 * 3600), departureZoneId = "Asia/Tokyo", arrivalZoneId = "Asia/Tokyo",
        departurePlace = "京都", arrivalPlace = "大阪", mode = TransportMode.TRAIN,
        departurePlatform = "5番線", arrivalPlatform = "4番線", trainLine = "京都線", trainType = TrainType.SPECIAL_RAPID)

    @Test fun busEditorOffersAllTypesAndSavesItsLine() {
        var saved: TransportLeg? = null
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            LegEditorScreen(1, listOf(trip), leg.copy(mode = TransportMode.BUS, busLine = "旧路線", busType = BusType.LOCAL),
                null, "Asia/Tokyo", false, false, null, {}, { value, _ -> saved = value }, null)
        } }
        compose.onNodeWithText("旧路線").performScrollTo().performTextReplacement("東京・大阪線")
        compose.onNodeWithText("路線バス").performScrollTo().performClick()
        compose.onNodeWithText("高速バス(昼行)").assertIsDisplayed()
        compose.onNodeWithText("高速バス(夜行)").performClick()
        compose.onNodeWithText("保存").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("東京・大阪線", saved!!.busLine)
            assertEquals(BusType.HIGHWAY_NIGHT, saved!!.busType)
        }
    }
    @Test fun lastTimelineItemCanClearTheWholeBackdrop() {
        val legs = (1L..5L).map { id -> leg.copy(id = id, sortOrder = id.toInt(),
            departure = start.plusSeconds((id - 10) * 3600), arrival = start.plusSeconds((id - 10) * 3600 + 1800),
            departurePlace = if (id == 5L) "最後の予定" else "途中$id") }
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            TimelineScreen(TripWithLegs(trip, legs), UserSettings(), {}, {}, {}, { _, _ -> }, { _, _ -> })
        } }
        compose.onNodeWithTag("timeline-list").performScrollToIndex(8)
        val last = compose.onNodeWithText("最後の予定 → 大阪", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("timeline-list").fetchSemanticsNode().boundsInRoot
        val density = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertTrue("Final item must scroll above even the taller footer backdrop", last.bottom <= viewport.bottom - 192 * density)
    }

    @Test fun routeIsLargestFirstAndRelativeDatesDoNotMoveTheRail() {
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            TimelineScreen(TripWithLegs(trip, listOf(leg)), UserSettings(), {}, {}, {}, { _, _ -> }, { _, _ -> })
        } }
        val route = compose.onNodeWithText("京都 → 大阪", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val boarding = compose.onNodeWithText("乗り場:5番線", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val service = compose.onNodeWithText("京都線 新快速", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(route.bottom <= boarding.top); assertTrue(boarding.bottom <= service.top)
        compose.onNodeWithText("降り場:4番線", useUnmergedTree = true).assertDoesNotExist()
        val rail = compose.onNodeWithTag("timeline-rail-1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.left
        val relative = compose.onNodeWithText("翌々07:41", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(relative.right <= rail)
        compose.onNodeWithText("2").performClick()
        val otherRail = compose.onNodeWithTag("timeline-rail-1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.left
        assertEquals(rail, otherRail, 0.1f)
        assertTrue(compose.onNodeWithText("前19:41", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.right <= rail)
    }
    @Test fun nextMovementUsesRouteBoardingArrivalServiceOrder() {
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            HomeScreen(listOf(TripWithLegs(trip, listOf(leg))), {}, { _, _ -> })
        } }
        val route = compose.onNodeWithText("京都 → 大阪", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val boarding = compose.onNodeWithText("乗り場:5番線", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val arrival = compose.onNodeWithText("降り場:4番線", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val service = compose.onNodeWithText("京都線 新快速", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(route.bottom <= boarding.top); assertTrue(boarding.bottom <= arrival.top); assertTrue(arrival.bottom <= service.top)
        compose.onNodeWithText("19:41", useUnmergedTree = true).assertIsDisplayed()
    }
    @Test fun importMenuIsAboveThePermanentTravelAddButton() {
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            Box(Modifier.fillMaxSize()) {
                AppBottomBar(AppDestination.PLANS, Modifier.align(Alignment.BottomCenter), onAdd = {}, onSelect = {})
                TripImportActions(true, {})
            }
        } }
        val add = compose.onNodeWithContentDescription("追加").fetchSemanticsNode().boundsInRoot.center
        val menu = compose.onNodeWithContentDescription("追加メニューを開く").fetchSemanticsNode().boundsInRoot.center
        assertEquals(add.x, menu.x, 1f)
        assertTrue(menu.y < add.y)
        compose.onNodeWithContentDescription("追加メニューを開く").performClick()
        compose.onNodeWithText("AIに考えてもらう").assertIsDisplayed()
        compose.onNodeWithText("JSONをコピペで読み込む").assertIsDisplayed()
        compose.onNodeWithText("JSONを読み込む").assertIsDisplayed()
    }

    @Test fun cardHasComfortableMinimumAndExpandsForLongRoutesNotHiddenMemo() {
        var shown by mutableStateOf(leg)
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            TimelineScreen(TripWithLegs(trip, listOf(shown)), UserSettings(), {}, {}, {}, { _, _ -> }, { _, _ -> })
        } }
        val short = compose.onNodeWithTag("timeline-rail-1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height
        val density = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertTrue("Minimum must be comfortable without keeping the old 132dp height", short >= 96 * density && short <= 110 * density)
        compose.runOnIdle { shown = leg.copy(memo = (1..8).joinToString("\n") { "メモ $it" }) }
        val unchanged = compose.onNodeWithTag("timeline-rail-1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height
        assertEquals(short, unchanged, 0.1f)
        compose.onNodeWithText(shown.memo, useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { shown = shown.copy(departurePlace = "とても長い出発地名を折り返してすべて表示するための駅名".repeat(3)) }
        val tall = compose.onNodeWithTag("timeline-rail-1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height
        assertTrue("Long routes should expand the card", tall > short + 26 * density)
    }
}
