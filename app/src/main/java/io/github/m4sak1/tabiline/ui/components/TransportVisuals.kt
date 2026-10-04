package io.github.m4sak1.tabiline.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Train
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.m4sak1.tabiline.core.model.TransportMode

data class TransportVisual(val icon: ImageVector, val color: Color)

@Composable
fun TransportMode.visual(): TransportVisual = when (this) {
    TransportMode.TRAIN -> TransportVisual(Icons.Rounded.Train, Color(0xFF4467C4))
    TransportMode.FLIGHT -> TransportVisual(Icons.Rounded.Flight, Color(0xFF8056C7))
    TransportMode.FERRY -> TransportVisual(Icons.Rounded.DirectionsBoat, Color(0xFF0089A5))
    TransportMode.BUS -> TransportVisual(Icons.Rounded.DirectionsBus, Color(0xFFD26722))
    TransportMode.OTHER -> TransportVisual(Icons.Rounded.Route, Color(0xFF4B8065))
}
