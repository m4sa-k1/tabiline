package io.github.m4sak1.tabiline.feature.timeline

import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.ui.components.*
import androidx.compose.ui.unit.dp
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class TransportPresentationTest {
    private val leg = TransportLeg(tripId = 1, departure = Instant.EPOCH,
        arrival = Instant.EPOCH.plusSeconds(3600), departureZoneId = "Asia/Tokyo",
        arrivalZoneId = "Asia/Tokyo", departurePlace = "京都", arrivalPlace = "大阪",
        mode = TransportMode.TRAIN, trainLine = "京都線", trainType = TrainType.SPECIAL_RAPID,
        departurePlatform = "5番線", arrivalPlatform = "4番線")

    @Test fun boardingAndServiceLabelsKeepRequestedOrder() {
        assertEquals(listOf("乗り場:5番線", "降り場:4番線"), leg.boardingLines)
        assertEquals("京都線 新快速", leg.serviceLabel)
        assertTrue(leg.copy(departurePlatform = "", arrivalPlatform = "").boardingLines.isEmpty())
    }
    @Test fun busLinesAndAllCategoriesAreShown() {
        BusType.entries.forEach { type ->
            assertEquals("東京・大阪線 ${type.label}", leg.copy(mode = TransportMode.BUS,
                busLine = "東京・大阪線", busType = type).serviceLabel)
        }
    }
    @Test fun blurModesAndMenuUseFixedFooterGeometry() {
        assertEquals(140.dp, FooterBlurMode.FOOTER_ONLY.backdropHeight(144.dp))
        assertEquals(192.dp, FooterBlurMode.WITH_ADD_BUTTON.backdropHeight(144.dp))
        assertEquals(54.dp, FooterLayout.menuEndPadding(390.dp))
    }
}
