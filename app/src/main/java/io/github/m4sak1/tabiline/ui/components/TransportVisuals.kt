package io.github.m4sak1.tabiline.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.m4sak1.tabiline.core.model.TransportMode

data class TransportVisual(val icon: ImageVector, val color: Color, val container: Color)

val TransportMode.detailLabel: String
    get() = if (this == TransportMode.FERRY) "フェリー" else label

@Composable
fun TransportMode.visual(): TransportVisual = when (this) {
    TransportMode.TRAIN -> TransportVisual(Icons.Rounded.Train, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    TransportMode.FLIGHT -> TransportVisual(Icons.Rounded.Flight, MaterialTheme.colorScheme.onTertiaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
    TransportMode.FERRY -> TransportVisual(Icons.Rounded.DirectionsBoat, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
    TransportMode.BUS -> TransportVisual(Icons.Rounded.DirectionsBus, MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
    TransportMode.OTHER -> TransportVisual(Icons.Rounded.MoreHoriz, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceContainerHighest)
}
