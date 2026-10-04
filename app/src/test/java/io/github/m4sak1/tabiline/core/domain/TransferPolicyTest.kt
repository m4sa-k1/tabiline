package io.github.m4sak1.tabiline.core.domain

import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.core.model.UserSettings
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferPolicyTest {
    private val base = Instant.parse("2026-10-04T01:00:00Z")

    @Test fun `flight connection under sixty minutes is tight`() {
        val result = TransferPolicy.evaluate(
            leg(arrival = base),
            leg(departure = base.plusSeconds(45 * 60), mode = TransportMode.FLIGHT),
            UserSettings(),
        )
        assertTrue(result is TransferStatus.Tight)
        assertEquals(45, result.minutes)
    }

    @Test fun `overlapping legs are an error across zones`() {
        val result = TransferPolicy.evaluate(
            leg(arrival = base.plusSeconds(30 * 60)),
            leg(departure = base),
            UserSettings(),
        )
        assertTrue(result is TransferStatus.Overlap)
        assertEquals(-30, result.minutes)
    }

    private fun leg(
        departure: Instant = base.minusSeconds(60),
        arrival: Instant = base.plusSeconds(60),
        mode: TransportMode = TransportMode.TRAIN,
    ) = TransportLeg(
        tripId = 1,
        departure = departure,
        arrival = arrival,
        departureZoneId = "Asia/Tokyo",
        arrivalZoneId = "Asia/Tokyo",
        departurePlace = "A",
        arrivalPlace = "B",
        mode = mode,
    )
}
