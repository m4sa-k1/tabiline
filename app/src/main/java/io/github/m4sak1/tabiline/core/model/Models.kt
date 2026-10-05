package io.github.m4sak1.tabiline.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class Trip(
    val id: Long = 0,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val note: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val isAutomatic: Boolean = false,
)

data class TransportLeg(
    val id: Long = 0,
    val tripId: Long,
    val departure: Instant,
    val arrival: Instant,
    val departureZoneId: String,
    val arrivalZoneId: String,
    val departurePlace: String,
    val arrivalPlace: String,
    val mode: TransportMode,
    val trainType: TrainType? = null,
    val trainLine: String = "",
    val departureTerminal: String = "",
    val arrivalTerminal: String = "",
    val boardingGroup: String = "",
    val flightNumber: String = "",
    val departurePlatform: String = "",
    val arrivalPlatform: String = "",
    val memo: String = "",
    val sortOrder: Int = 0,
    val precedingGapType: GapType = GapType.WAIT,
    val busLine: String = "",
    val busType: BusType? = null,
) {
    val departureLocal: ZonedDateTime get() = departure.atZone(ZoneId.of(departureZoneId))
    val arrivalLocal: ZonedDateTime get() = arrival.atZone(ZoneId.of(arrivalZoneId))
}

data class TripWithLegs(
    val trip: Trip,
    val legs: List<TransportLeg>,
)

enum class ThemePreference { SYSTEM, LIGHT, DARK }

enum class AccentPalette { PURPLE, ORCHID, BLUE, GREEN, CORAL, AMBER, TEAL, MONO }

enum class FooterBlurMode(val label: String) {
    FOOTER_ONLY("フッターのみ"),
    WITH_ADD_BUTTON("追加ボタンまで"),
}

enum class GapType(val label: String) {
    WAIT("待ち"),
    TRANSFER("移動"),
}

data class UserSettings(
    val notificationsEnabled: Boolean = false,
    val notificationMinutes: Map<TransportMode, Int> = TransportMode.entries.associateWith { if (it == TransportMode.FLIGHT) 60 else 10 },
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.PURPLE,
    val defaultZoneId: String = "Asia/Tokyo",
    val trainMinutes: Int = 10,
    val busMinutes: Int = 10,
    val flightMinutes: Int = 60,
    val ferryMinutes: Int = 30,
    val otherMinutes: Int = 15,
    val footerBlurEnabled: Boolean = true,
    val footerBlurMode: FooterBlurMode = FooterBlurMode.WITH_ADD_BUTTON,
) {
    fun thresholdFor(mode: TransportMode): Int = when (mode) {
        TransportMode.TRAIN -> trainMinutes
        TransportMode.BUS -> busMinutes
        TransportMode.FLIGHT -> flightMinutes
        TransportMode.FERRY -> ferryMinutes
        TransportMode.WALK -> otherMinutes
        TransportMode.FREE_TIME -> otherMinutes
        TransportMode.OTHER -> otherMinutes
    }
}
