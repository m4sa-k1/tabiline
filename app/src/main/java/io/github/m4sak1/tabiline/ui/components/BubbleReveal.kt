package io.github.m4sak1.tabiline.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.hypot
import kotlin.math.max

private const val bubbleMotionMillis = 360
private const val centerPopupMotionMillis = 280
private val bubbleEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Slides the full-size editor vertically without scaling, fading or overshoot. */
@Composable
fun EditorSlideTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onProgress: (Float) -> Unit = {},
    onHidden: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        onProgress(progress.value)
        progress.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(240, easing = bubbleEasing),
        ) { onProgress(value) }
        if (!visible) onHidden()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = size.height * (1f - progress.value)
            },
        content = content,
    )
}

/** Softly grows a compact popup from the center without directional travel. */
@Composable
fun CenterPopup(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onProgress: (Float) -> Unit = {},
    onHidden: () -> Unit = {},
    content: @Composable BoxScope.(progress: Float, motionModifier: Modifier) -> Unit,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        onProgress(progress.value)
        progress.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(centerPopupMotionMillis, easing = bubbleEasing),
        ) { onProgress(value) }
        if (!visible) onHidden()
    }

    Box(modifier.fillMaxSize()) {
        val motionModifier = Modifier.graphicsLayer {
            val scale = 0.86f + 0.14f * progress.value
            scaleX = scale
            scaleY = scale
            alpha = progress.value
            transformOrigin = TransformOrigin.Center
        }
        content(progress.value, motionModifier)
    }
}

/** Moves and scales a compact popup between its final position and the fixed add button. */
@Composable
fun BubblePopup(
    visible: Boolean,
    modifier: Modifier = Modifier,
    originXFraction: Float = 0.78f,
    originYFraction: Float = 0.91f,
    onProgress: (Float) -> Unit = {},
    onHidden: () -> Unit = {},
    content: @Composable BoxScope.(progress: Float, motionModifier: Modifier) -> Unit,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        onProgress(progress.value)
        progress.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(bubbleMotionMillis, easing = bubbleEasing),
        ) { onProgress(value) }
        if (!visible) onHidden()
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val rootWidth = constraints.maxWidth.toFloat()
        val rootHeight = constraints.maxHeight.toFloat()
        val scale = 0.12f + 0.88f * progress.value
        val motionModifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = (rootWidth * originXFraction - rootWidth / 2f) * (1f - progress.value)
            translationY = (rootHeight * originYFraction - rootHeight / 2f) * (1f - progress.value)
            transformOrigin = TransformOrigin.Center
            clip = true
        }
        content(progress.value, motionModifier)
    }
}
