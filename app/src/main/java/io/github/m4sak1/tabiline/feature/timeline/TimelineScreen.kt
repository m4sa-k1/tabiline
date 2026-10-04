package io.github.m4sak1.tabiline.feature.timeline

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FreeBreakfast
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.core.model.GapType
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.core.model.UserSettings
import io.github.m4sak1.tabiline.ui.components.detailLabel
import io.github.m4sak1.tabiline.ui.components.visual
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val timelineTime = DateTimeFormatter.ofPattern("HH:mm")
private val shortDateTime = DateTimeFormatter.ofPattern("M/d HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TimelineScreen(
    item: TripWithLegs?,
    settings: UserSettings,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
    onEditLeg: (Long) -> Unit,
    onMoveLeg: (Long, Int) -> Unit,
    onUpdateGapType: (Long, GapType) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val trip = item?.trip
    val dates = trip?.let { start -> generateSequence(start.startDate) { day -> day.plusDays(1).takeIf { !it.isAfter(start.endDate) } }.toList() }.orEmpty()
    val initialDay = trip?.let { if (LocalDate.now() in it.startDate..it.endDate) LocalDate.now() else it.startDate }
    var selectedEpochDay by rememberSaveable(trip?.id) { mutableStateOf(initialDay?.toEpochDay()) }
    val selectedDate = selectedEpochDay?.let(LocalDate::ofEpochDay) ?: initialDay
    val dayLegs = item?.legs.orEmpty().filter { it.departureLocal.toLocalDate() == selectedDate }.sortedBy { it.sortOrder }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        if (item == null || selectedDate == null) Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) { Text("読み込み中…") }
        else Box(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                Surface(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.trip.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "旅行メニュー") }
                        DropdownMenu(menuOpen, { menuOpen = false }) {
                            if (!item.trip.isAutomatic) {
                                DropdownMenuItem(text = { Text("旅行を編集") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menuOpen = false; onEditTrip() })
                            }
                            DropdownMenuItem(
                                text = { Text(if (item.trip.isAutomatic) "この日の移動を削除" else "旅行を削除") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                onClick = { menuOpen = false; confirmDelete = true },
                            )
                        }
                    }
                }
            }
                if (dates.size > 1) RelativeDaySelector(dates, selectedDate) { selectedEpochDay = it.toEpochDay() }
                if (dayLegs.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 144.dp),
                    ) {
                        dayLegs.forEachIndexed { index, leg ->
                            item(key = "leg-${leg.id}") {
                                TimelineLeg(
                                    leg = leg,
                                    hasPrevious = index > 0,
                                    hasNext = index < dayLegs.lastIndex,
                                    onClick = { onEditLeg(leg.id) },
                                    onMove = { onMoveLeg(leg.id, it) },
                                )
                            }
                            if (index < dayLegs.lastIndex) {
                                item(key = "wait-${leg.id}") {
                                    val next = dayLegs[index + 1]
                                    val minutes = Duration.between(leg.arrival, next.departure).toMinutes()
                                    TimelineGap(
                                        minutes = minutes,
                                        threshold = settings.thresholdFor(next.mode),
                                        type = next.precedingGapType,
                                        onTypeChange = { onUpdateGapType(next.id, it) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (dayLegs.isEmpty()) EmptyDay(Modifier.fillMaxSize())
        }
    }
    if (confirmDelete && item != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(if (item.trip.isAutomatic) "この日の移動を削除しますか？" else "旅行を削除しますか？") },
        text = { Text(if (item.trip.isAutomatic) "この日付にまとめられた移動がすべて削除されます。" else "この旅行に含まれる移動もすべて削除されます。") },
        confirmButton = { TextButton(onClick = onDeleteTrip) { Text("削除") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") } },
    )
}

@Composable
private fun RelativeDaySelector(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val selectorWidth = (dates.size * 72 + (dates.size - 1) * 4).coerceAtMost(360).dp
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        LazyRow(
            modifier = Modifier.width(selectorWidth),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(dates, key = { _, date -> date.toEpochDay() }) { index, date ->
                val selected = date == selectedDate
                val startRadius by animateDpAsState(
                    targetValue = if (selected || index == 0) 22.dp else 5.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "day-start-shape",
                )
                val endRadius by animateDpAsState(
                    targetValue = if (selected || index == dates.lastIndex) 22.dp else 5.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "day-end-shape",
                )
                Surface(
                    onClick = { onSelect(date) },
                    modifier = Modifier.size(width = 72.dp, height = 44.dp),
                    shape = RoundedCornerShape(
                        topStart = startRadius,
                        bottomStart = startRadius,
                        topEnd = endRadius,
                        bottomEnd = endRadius,
                    ),
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineLeg(
    leg: TransportLeg,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val visual = leg.mode.visual()
    var drag by remember { mutableFloatStateOf(0f) }
    Row(
        Modifier.fillMaxWidth().height(104.dp).zIndex(1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(62.dp).fillMaxHeight()) {
            Text(
                leg.departureLocal.format(timelineTime),
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart),
            )
            val arrival = if (leg.arrivalLocal.toLocalDate() == leg.departureLocal.toLocalDate()) leg.arrivalLocal.format(timelineTime)
                else leg.arrivalLocal.format(shortDateTime)
            Text(
                arrival,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.align(Alignment.BottomStart),
                maxLines = 1,
            )
        }
        Box(Modifier.width(24.dp).fillMaxHeight()) {
            if (hasPrevious) {
                Box(
                    Modifier.align(Alignment.TopCenter).width(4.dp).height(13.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
            Box(Modifier.align(Alignment.TopCenter).offset(y = 13.dp).width(4.dp).height(80.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
            if (hasNext) {
                Box(
                    Modifier.align(Alignment.TopCenter).offset(y = 93.dp).width(4.dp).height(11.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
            Box(Modifier.align(Alignment.TopCenter).offset(y = 6.dp).size(14.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp)))
            Box(Modifier.align(Alignment.TopCenter).offset(y = 86.dp).size(14.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp)))
        }
        Card(
            onClick = onClick,
            modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp)
                .pointerInput(leg.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { drag = .1f },
                        onDrag = { change, amount -> change.consume(); drag += amount.y },
                        onDragEnd = { if (kotlin.math.abs(drag) > 36f) onMove(if (drag < 0) -1 else 1); drag = 0f },
                        onDragCancel = { drag = 0f },
                    )
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    val route = "${leg.departurePlace} → ${leg.arrivalPlace}"
                    val hasPlatform = leg.departurePlatform.isNotBlank()
                    val title = leg.departurePlatform.ifBlank { route }
                    Text(
                        title,
                        fontSize = if (hasPlatform) 18.sp else 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val type = if (leg.mode == TransportMode.TRAIN) leg.trainType?.label ?: leg.mode.detailLabel else leg.mode.detailLabel
                    if (hasPlatform) {
                        Text(
                            route,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        listOfNotNull(type, leg.memo.takeIf(String::isNotBlank)).joinToString(" ・ "),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    modifier = Modifier.padding(start = 12.dp).size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 2.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(visual.icon, leg.mode.detailLabel, Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineGap(
    minutes: Long,
    threshold: Int,
    type: GapType,
    onTypeChange: (GapType) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val warning = type != GapType.FREE_TIME && (minutes < 0 || minutes < threshold)
    val title = when {
        minutes < 0 -> "時刻が ${-minutes}分 重複"
        warning -> "${type.label} ${minutes}分 ・ 乗り継ぎに注意"
        else -> "${type.label} ${minutes}分"
    }
    val subtitle = when (type) {
        GapType.WAIT -> "同じ乗り場で待機"
        GapType.TRANSFER -> "異なる乗り場へ移動"
        GapType.FREE_TIME -> "自由に使える時間"
    }
    Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(62.dp))
        Box(Modifier.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(4.dp).fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(2.dp)))
        }
        Surface(
            onClick = { pickerOpen = true },
            modifier = Modifier.weight(1f).height(64.dp).padding(start = 12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(22.dp),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (warning) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = if (warning) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (warning) Icons.Rounded.Warning else type.icon(), null, Modifier.size(24.dp))
                    }
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 20.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (pickerOpen) AlertDialog(
        onDismissRequest = { pickerOpen = false },
        title = { Text("間の過ごし方") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GapType.entries.forEach { choice ->
                    Surface(
                        onClick = {
                            onTypeChange(choice)
                            pickerOpen = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = if (choice == type) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(choice.icon(), null, Modifier.size(24.dp))
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(choice.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    when (choice) {
                                        GapType.WAIT -> "同じ乗り場で待つ"
                                        GapType.TRANSFER -> "別の乗り場へ移動する"
                                        GapType.FREE_TIME -> "自由に使える時間にする"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { pickerOpen = false }) { Text("キャンセル") }
        },
    )
}

private fun GapType.icon() = when (this) {
    GapType.WAIT -> Icons.Rounded.Schedule
    GapType.TRANSFER -> Icons.Rounded.SyncAlt
    GapType.FREE_TIME -> Icons.Rounded.FreeBreakfast
}

@Composable
private fun EmptyDay(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "この日の移動はありません",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            "追加するとタイムラインに表示されます",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center).offset(y = 38.dp),
        )
    }
}
