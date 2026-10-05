package io.github.m4sak1.tabiline.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FreeBreakfast
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.core.model.TransportLeg

data class TransportVisual(val icon: ImageVector, val color: Color, val container: Color)

val TransportMode.detailLabel: String
    get() = if (this == TransportMode.FERRY) "フェリー" else label

val TransportLeg.serviceLabel: String
    get() = if (mode == TransportMode.TRAIN) {
        listOfNotNull(
            trainLine.takeIf(String::isNotBlank),
            trainType?.label,
        ).joinToString(" ").ifBlank { mode.detailLabel }
    } else if (mode == TransportMode.FLIGHT) {
        listOf(mode.detailLabel, flightNumber, boardingGroup.takeIf(String::isNotBlank)?.let { "Group $it" }.orEmpty())
            .filter(String::isNotBlank).joinToString(" ・ ")
    } else if (mode == TransportMode.BUS) {
        listOfNotNull(busLine.takeIf(String::isNotBlank), busType?.label)
            .joinToString(" ").ifBlank { mode.detailLabel }
    } else {
        mode.detailLabel
    }

val TransportLeg.departureBoardingLabel: String
    get() = if (mode == TransportMode.FLIGHT) {
        listOfNotNull(
            departureTerminal.takeIf(String::isNotBlank)?.let { "ターミナル $it" },
            departurePlatform.takeIf(String::isNotBlank)?.let { "ゲート $it" },
        ).joinToString(" ・ ")
    } else departurePlatform

val TransportLeg.arrivalBoardingLabel: String
    get() = if (mode == TransportMode.FLIGHT) {
        listOfNotNull(
            arrivalTerminal.takeIf(String::isNotBlank)?.let { "ターミナル $it" },
            arrivalPlatform.takeIf(String::isNotBlank)?.let { "ゲート $it" },
        ).joinToString(" ・ ")
    } else arrivalPlatform

val TransportLeg.boardingLines: List<String>
    get() = listOfNotNull(
        departureBoardingLabel.takeIf(String::isNotBlank)?.let { "乗り場:$it" },
        arrivalBoardingLabel.takeIf(String::isNotBlank)?.let { "降り場:$it" },
    )

@Composable
fun TransportMode.visual(): TransportVisual = when (this) {
    TransportMode.TRAIN -> TransportVisual(Icons.Rounded.Train, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    TransportMode.FLIGHT -> TransportVisual(Icons.Rounded.Flight, MaterialTheme.colorScheme.onTertiaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
    TransportMode.FERRY -> TransportVisual(Icons.Rounded.DirectionsBoat, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
    TransportMode.BUS -> TransportVisual(Icons.Rounded.DirectionsBus, MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
    TransportMode.WALK -> TransportVisual(Icons.AutoMirrored.Rounded.DirectionsWalk, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    TransportMode.FREE_TIME -> TransportVisual(Icons.Rounded.FreeBreakfast, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
    TransportMode.OTHER -> TransportVisual(Icons.Rounded.MoreHoriz, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceContainerHighest)
}
