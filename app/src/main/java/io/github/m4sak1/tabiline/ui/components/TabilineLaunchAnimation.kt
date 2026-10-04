package io.github.m4sak1.tabiline.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import io.github.m4sak1.tabiline.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val LaunchEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun TabilineLaunchAnimation(
    started: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val routeProgress = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val context = LocalContext.current
    val iconColor = remember { AppIconChoice.current(context).color }
    var readyToAnimate by remember { mutableStateOf(started) }
    var animationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(started) {
        if (started) readyToAnimate = true
    }
    LaunchedEffect(Unit) {
        // A few vendor implementations never dispatch the platform splash exit
        // callback. Never let that leave the branded overlay on screen forever.
        delay(1_000)
        readyToAnimate = true
    }
    LaunchedEffect(readyToAnimate) {
        if (!readyToAnimate || animationStarted) return@LaunchedEffect
        animationStarted = true
        // Some Android builds keep the outgoing splash surface for a few frames
        // after the callback. Holding this branded first frame prevents the
        // route animation from finishing behind that surface.
        delay(520)
        routeProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 660, easing = LaunchEasing),
        )
        delay(280)
        exitProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = LaunchEasing),
        )
        onFinished()
    }
    LaunchedEffect(Unit) {
        // Final safety net for interrupted or non-conforming animation clocks.
        delay(4_500)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = -size.height * exitProgress.value
            }
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(width = 112.dp, height = 168.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                val primary = iconColor
                val quiet = iconColor.copy(alpha = 0.25f)
                Canvas(Modifier.fillMaxSize()) {
                    val x = size.width / 2f
                    val top = with(density) { 22.dp.toPx() }
                    val bottom = with(density) { 136.dp.toPx() }
                    val currentY = top + (bottom - top) * routeProgress.value
                    drawLine(
                        color = quiet,
                        start = Offset(x, top),
                        end = Offset(x, bottom),
                        strokeWidth = with(density) { 8.dp.toPx() },
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = primary,
                        start = Offset(x, top),
                        end = Offset(x, currentY),
                        strokeWidth = with(density) { 8.dp.toPx() },
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = primary,
                        radius = with(density) { 8.dp.toPx() },
                        center = Offset(x, top),
                    )
                    drawCircle(
                        color = primary,
                        radius = with(density) { 8.dp.toPx() },
                        center = Offset(x, currentY),
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(y = 42.dp + 76.dp * routeProgress.value)
                        .size(64.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(iconColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notification),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Tabiline",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "旅の移動を、一本の線に。",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
