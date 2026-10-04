package io.github.m4sak1.tabiline.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppDestination { TODAY, TIMELINE, PLANS, SETTINGS }

@Composable
fun AppBottomBar(
    selected: AppDestination,
    onAdd: (() -> Unit)? = null,
    addContentDescription: String = "追加",
    addText: String? = null,
    onSelect: (AppDestination) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FooterButton(AppDestination.TODAY, selected, Icons.Rounded.Today, "今日", onSelect)
                FooterButton(AppDestination.TIMELINE, selected, Icons.Rounded.Timeline, "タイムライン", onSelect)
                FooterButton(AppDestination.PLANS, selected, Icons.Rounded.Luggage, "旅行", onSelect)
                FooterButton(AppDestination.SETTINGS, selected, Icons.Rounded.Settings, "設定", onSelect)
            }
            if (onAdd != null) {
                Surface(
                    onClick = onAdd,
                    modifier = Modifier.size(104.dp),
                    shape = RoundedCornerShape(34.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
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
private fun FooterButton(
    destination: AppDestination,
    selected: AppDestination,
    icon: ImageVector,
    contentDescription: String,
    onSelect: (AppDestination) -> Unit,
) {
    val isSelected = destination == selected
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "footer-container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "footer-content",
    )
    val startRadius by animateDpAsState(
        targetValue = if (isSelected || destination == AppDestination.TODAY) 26.dp else 5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "footer-start-shape",
    )
    val endRadius by animateDpAsState(
        targetValue = if (isSelected || destination == AppDestination.SETTINGS) 26.dp else 5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
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
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
        )
    }
}
