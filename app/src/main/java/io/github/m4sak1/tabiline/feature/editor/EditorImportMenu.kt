package io.github.m4sak1.tabiline.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorImportMenu(enabled: Boolean, onAi: () -> Unit, onJson: () -> Unit, onPaste: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    BackHandler(expanded) { expanded = false }
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    // M3 standard easing with short/medium durations; monotonically interpolated without rebound.
    // Keep the current stable Material library instead of adopting an alpha-only FAB menu API.
    val easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val radius by animateDpAsState(if (expanded) 28.dp else 16.dp, tween(200, easing = easing), label = "menuShape")
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, tween(200, easing = easing), label = "menuIcon")
    val color by animateColorAsState(if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        tween(200, easing = easing), label = "menuColor")
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(expanded, enter = expandVertically(tween(300, easing = easing), expandFrom = Alignment.Bottom),
            exit = shrinkVertically(tween(200, easing = easing), shrinkTowards = Alignment.Bottom)) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { expanded = false; onAi() }, enabled = enabled, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(10.dp)); Text("AIに考えてもらう")
                }
                FilledTonalButton(onClick = { expanded = false; onPaste() }, enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                    Icon(Icons.Rounded.ContentPaste, null); Spacer(Modifier.width(10.dp)); Text("JSONをコピペで読み込む")
                }
                FilledTonalButton(onClick = { expanded = false; onJson() }, enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                    Icon(Icons.Rounded.DataObject, null); Spacer(Modifier.width(10.dp)); Text("JSONを読み込む")
                }
            }
        }
        Surface(onClick = { expanded = !expanded }, enabled = enabled, shape = RoundedCornerShape(radius),
            color = color, contentColor = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(56.dp).semantics { stateDescription = if (expanded) "開いています" else "閉じています" }) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, if (expanded) "追加メニューを閉じる" else "追加メニューを開く",
                    Modifier.size(24.dp).graphicsLayer { rotationZ = rotation })
            }
        }
    }
}
