package io.github.m4sak1.tabiline.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.feature.home.HomeScreen
import io.github.m4sak1.tabiline.ui.components.AppBottomBar
import io.github.m4sak1.tabiline.ui.components.AppDestination
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** Real production screen + footer; isolated fixture, no database/settings writes. */
class PromoHomeCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun captureHome() {
        compose.activity.runOnUiThread {
            compose.activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        }
        val date = LocalDate.now().plusDays(1)
        val zone = ZoneId.of("Asia/Tokyo")
        fun leg(id: Long, hour: Int, minute: Int, endHour: Int, endMinute: Int, from: String, to: String, mode: TransportMode, platform: String, line: String, type: TrainType?, gap: GapType) = TransportLeg(
            id = id, tripId = 1, departure = date.atTime(hour, minute).atZone(zone).toInstant(),
            arrival = date.atTime(endHour, endMinute).atZone(zone).toInstant(), departureZoneId = zone.id, arrivalZoneId = zone.id,
            departurePlace = from, arrivalPlace = to, mode = mode, trainType = type, trainLine = line, departurePlatform = platform,
            memo = if (id == 1L) "のぞみ / 6号車 12A" else "", sortOrder = (id - 1).toInt(), precedingGapType = gap)
        val trip = TripWithLegs(Trip(1, "京都旅行", date, date.plusDays(1)), listOf(
            leg(1, 9, 0, 11, 15, "東京", "京都", TransportMode.TRAIN, "18番線", "東海道", TrainType.SHINKANSEN, GapType.WAIT),
            leg(2, 11, 23, 11, 28, "京都", "稲荷", TransportMode.TRAIN, "9番線", "奈良線", TrainType.LOCAL, GapType.TRANSFER),
            leg(3, 11, 45, 11, 50, "稲荷駅", "伏見稲荷大社", TransportMode.WALK, "", "", null, GapType.WAIT)))
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                HomeScreen(listOf(trip), {}, { _, _ -> })
                AppBottomBar(selected = AppDestination.TODAY, modifier = Modifier.align(Alignment.BottomCenter), onAdd = {}, onSelect = {})
            }
        } }
        compose.onNodeWithText("18番線").assertIsDisplayed()
        compose.onNodeWithText("京都 → 稲荷", substring = true).assertExists()
        compose.mainClock.autoAdvance = false // Freeze the real countdown while capturing a frame.
        Thread.sleep(500)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val target = File(instrumentation.targetContext.getExternalFilesDir(null), "promo/home.png")
        target.parentFile!!.mkdirs()
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        compose.mainClock.autoAdvance = true
    }
}
