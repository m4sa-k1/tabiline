package io.github.m4sak1.tabiline.feature.timeline

import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.ui.components.departureBoardingLabel
import io.github.m4sak1.tabiline.ui.components.serviceLabel
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class FlightLabelsTest {
    private val flight = TransportLeg(
        tripId = 1, departure = Instant.EPOCH, arrival = Instant.EPOCH.plusSeconds(3600),
        departureZoneId = "Asia/Tokyo", arrivalZoneId = "Asia/Tokyo",
        departurePlace = "羽田", arrivalPlace = "高松", mode = TransportMode.FLIGHT,
        departureTerminal = "2", arrivalTerminal = "国内線",
        departurePlatform = "64", arrivalPlatform = "3",
        flightNumber = "NH533", boardingGroup = "2",
    )

    @Test fun `list includes departure only and flight number`() {
        assertEquals("ターミナル 2 ・ ゲート 64", flight.departureBoardingLabel)
        assertEquals("飛行機 ・ NH533 ・ Group 2", flight.serviceLabel)
        assertFalse(flight.departureBoardingLabel.contains("国内線"))
    }

    @Test fun `optional fields and old gate data remain usable`() {
        assertEquals("ゲート 64", flight.copy(departureTerminal = "").departureBoardingLabel)
        assertEquals("", flight.copy(departureTerminal = "", departurePlatform = "").departureBoardingLabel)
        assertEquals("飛行機", flight.copy(flightNumber = "", boardingGroup = "").serviceLabel)
    }

    @Test fun `other modes ignore flight fields`() {
        val bus = flight.copy(mode = TransportMode.BUS)
        assertEquals("64", bus.departureBoardingLabel)
        assertEquals("バス", bus.serviceLabel)
    }
}
