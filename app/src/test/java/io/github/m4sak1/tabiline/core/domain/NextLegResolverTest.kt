package io.github.m4sak1.tabiline.core.domain

import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class NextLegResolverTest {
    private val now = Instant.parse("2026-10-04T01:00:00Z")

    @Test fun `current leg wins over a later leg`() {
        val current = leg(1, now.minusSeconds(60), now.plusSeconds(60))
        val later = leg(2, now.plusSeconds(120), now.plusSeconds(180))
        val result = NextLegResolver.resolve(listOf(later, current), now)
        assertEquals(LegMoment.CURRENT, result.moment)
        assertEquals(1L, result.leg?.id)
    }

    @Test fun `earliest future departure is selected regardless of display order`() {
        val later = leg(1, now.plusSeconds(300), now.plusSeconds(400))
        val next = leg(2, now.plusSeconds(120), now.plusSeconds(180))
        assertEquals(2L, NextLegResolver.resolve(listOf(later, next), now).leg?.id)
    }

    private fun leg(id: Long, departure: Instant, arrival: Instant) = TransportLeg(
        id = id, tripId = 1, departure = departure, arrival = arrival,
        departureZoneId = "UTC", arrivalZoneId = "UTC",
        departurePlace = "A", arrivalPlace = "B", mode = TransportMode.TRAIN,
    )
}
