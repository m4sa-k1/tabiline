package io.github.m4sak1.tabiline.feature.timeline

import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class TimelineDatesTest {
    private val first = LocalDate.of(2026, 10, 31)

    @Test fun `relative labels stay on one line across month boundaries`() {
        val time = first.atTime(7, 48)
        assertEquals("07:48", relativeTimelineTime(time, first))
        assertEquals("翌07:48", relativeTimelineTime(time.plusDays(1), first))
        assertEquals("翌々07:48", relativeTimelineTime(time.plusDays(2), first))
        assertEquals("前19:41", relativeTimelineTime(first.minusDays(1).atTime(19, 41), first))
        assertEquals("前々19:41", relativeTimelineTime(first.minusDays(2).atTime(19, 41), first))
        assertEquals("3日後07:48", relativeTimelineTime(time.plusDays(3), first))
    }

    @Test fun `overnight leg appears on departure intermediate and arrival days only`() {
        val leg = leg(first.atTime(19, 41), first.plusDays(2).atTime(7, 48))
        assertFalse(leg.isVisibleOn(first.minusDays(1)))
        assertTrue(leg.isVisibleOn(first))
        assertTrue(leg.isVisibleOn(first.plusDays(1)))
        assertTrue(leg.isVisibleOn(first.plusDays(2)))
        assertFalse(leg.isVisibleOn(first.plusDays(3)))
    }

    @Test fun `same day leg is not carried into next day`() {
        val leg = leg(first.atTime(10, 0), first.atTime(11, 0))
        assertTrue(leg.isVisibleOn(first))
        assertFalse(leg.isVisibleOn(first.plusDays(1)))
    }

    private fun leg(start: LocalDateTime, end: LocalDateTime) = TransportLeg(
        tripId = 1,
        departure = start.atZone(ZoneId.of("Asia/Tokyo")).toInstant(),
        arrival = end.atZone(ZoneId.of("Asia/Tokyo")).toInstant(),
        departureZoneId = "Asia/Tokyo",
        arrivalZoneId = "Asia/Tokyo",
        departurePlace = "A",
        arrivalPlace = "B",
        mode = TransportMode.BUS,
    )
}
