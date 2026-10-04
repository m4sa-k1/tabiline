package io.github.m4sak1.tabiline.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.TransportLeg
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun LegCard(
    leg: TransportLeg,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: (Int) -> Unit = {},
) {
    val visual = leg.mode.visual()
    var dragY by remember { mutableFloatStateOf(0f) }
    val container = if (highlighted) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .scale(if (dragY != 0f) 1.015f else 1f)
            .pointerInput(leg.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragY = 0.1f },
                    onDrag = { change, amount -> change.consume(); dragY += amount.y },
                    onDragEnd = { if (kotlin.math.abs(dragY) > 36f) onMove(if (dragY < 0) -1 else 1); dragY = 0f },
                    onDragCancel = { dragY = 0f },
                )
            },
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 16.dp, bottomEnd = 28.dp, bottomStart = 16.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(visual.color.copy(alpha = .16f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(visual.icon, leg.mode.label, tint = visual.color) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        buildString {
                            append(leg.mode.detailLabel)
                            leg.trainType?.let { append("・${it.label}") }
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = visual.color,
                    )
                    if (leg.memo.isNotBlank()) Text(
                        leg.memo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(Icons.Rounded.DragIndicator, "長押しして並べ替え", tint = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(14.dp))
            JourneyPoint(leg.departureLocal.format(timeFormatter), leg.departurePlace, leg.departurePlatform)
            Box(Modifier.padding(start = 37.dp).height(18.dp).width(2.dp).background(visual.color.copy(alpha = .45f)))
            JourneyPoint(leg.arrivalLocal.format(timeFormatter), leg.arrivalPlace, leg.arrivalPlatform)
        }
    }
}

@Composable
private fun JourneyPoint(time: String, place: String, platform: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(time, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(place, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (platform.isNotBlank()) Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Rounded.LocationOn, null, Modifier.size(14.dp))
                    Text(platform, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
