package io.github.m4sak1.tabiline.notifications

import io.github.m4sak1.tabiline.core.model.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class DepartureRemindersTest {
    private val leg = TransportLeg(tripId = 1, departure = Instant.parse("2026-11-01T00:05:00Z"),
        arrival = Instant.parse("2026-11-01T02:00:00Z"), departureZoneId = "Asia/Tokyo",
        arrivalZoneId = "Europe/London", departurePlace = "A", arrivalPlace = "B", mode = TransportMode.FLIGHT)
    private val settings = UserSettings(notificationsEnabled = true)

    @Test fun `notifications disabled by default`() { assertFalse(UserSettings().notificationsEnabled) }
    @Test fun `flight reminder crosses date boundary using instant`() {
        assertEquals(Instant.parse("2026-10-31T23:05:00Z"), reminderTime(leg, settings))
    }
    @Test fun `each mode has independent offset and zero means departure`() {
        val updated = settings.copy(notificationMinutes = settings.notificationMinutes + (TransportMode.TRAIN to 25) + (TransportMode.FLIGHT to 0))
        assertEquals(leg.departure, reminderTime(leg, updated))
        assertEquals(leg.departure.minusSeconds(1500), reminderTime(leg.copy(mode = TransportMode.TRAIN), updated))
    }
    @Test fun `receiver rejects disabled edited early and expired reminders`() {
        val at = reminderTime(leg, settings)
        assertTrue(shouldDeliver(leg, settings, at.toEpochMilli(), at))
        assertFalse(shouldDeliver(leg, settings.copy(notificationsEnabled = false), at.toEpochMilli(), at))
        assertFalse(shouldDeliver(leg.copy(departure = leg.departure.plusSeconds(60)), settings, at.toEpochMilli(), at))
        assertFalse(shouldDeliver(leg, settings, at.toEpochMilli(), at.minusSeconds(1)))
        assertFalse(shouldDeliver(leg, settings, at.toEpochMilli(), leg.departure.plusSeconds(60)))
    }
}
