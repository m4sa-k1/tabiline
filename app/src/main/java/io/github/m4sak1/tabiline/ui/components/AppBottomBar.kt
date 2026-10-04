package io.github.m4sak1.tabiline.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppDestination { TODAY, TIMELINE, PLANS, SETTINGS }

@Composable
fun AppBottomBar(
    selected: AppDestination,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
    addContentDescription: String = "追加",
    addText: String? = null,
    onSelect: (AppDestination) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            FooterSelector(selected = selected, onSelect = onSelect)
            if (onAdd != null) {
                Surface(
                    onClick = onAdd,
                    modifier = Modifier.size(104.dp),
                    shape = RoundedCornerShape(34.dp),
                    color = if (addText == null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (addText == null) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.primary,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                        if (addText == null) {
                            Icon(Icons.Rounded.Add, addContentDescription, Modifier.size(40.dp))
                        } else {
                            Text(
                                text = addText,
                                fontSize = when {
                                    addText.length >= 10 -> 13.sp
                                    addText.length >= 7 -> 15.sp
                                    else -> 18.sp
                                },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterSelector(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
) {
    val destinations = listOf(
        Triple(AppDestination.TODAY, Icons.Rounded.Today, "今日"),
        Triple(AppDestination.TIMELINE, Icons.Rounded.Timeline, "タイムライン"),
        Triple(AppDestination.PLANS, Icons.Rounded.Luggage, "旅行"),
        Triple(AppDestination.SETTINGS, Icons.Rounded.Settings, "設定"),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        destinations.forEachIndexed { index, (destination, icon, description) ->
            val isSelected = destination == selected
            val startRadius by animateDpAsState(
                targetValue = if (isSelected || index == 0) 26.dp else 5.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "footer-start-shape",
            )
            val endRadius by animateDpAsState(
                targetValue = if (isSelected || index == destinations.lastIndex) 26.dp else 5.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "footer-end-shape",
            )
            IconButton(
                onClick = { onSelect(destination) },
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(
                    topStart = startRadius,
                    bottomStart = startRadius,
                    topEnd = endRadius,
                    bottomEnd = endRadius,
                ),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(icon, description, Modifier.size(22.dp))
            }
        }
    }
}
