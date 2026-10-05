package io.github.m4sak1.tabiline.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Samples navigation content only. Controls stay crisp and the upper boundary fades out. */
@Composable
fun FooterBackdrop(state: HazeState, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    val fade = with(LocalDensity.current) { 48.dp.toPx() }
    Box(modifier.hazeEffect(state) {
        backgroundColor = surface
        blurRadius = 24.dp
        noiseFactor = 0f
        tints = emptyList()
        // Older devices without blur support must not receive an opaque footer background.
        fallbackTint = HazeTint(Color.Transparent)
        mask = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = fade)
    })
}
