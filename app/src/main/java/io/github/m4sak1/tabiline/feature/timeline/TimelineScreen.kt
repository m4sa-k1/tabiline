package io.github.m4sak1.tabiline.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.core.model.UserSettings
import io.github.m4sak1.tabiline.ui.components.AppBottomBar
import io.github.m4sak1.tabiline.ui.components.AppDestination
import io.github.m4sak1.tabiline.ui.components.detailLabel
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timelineTime = DateTimeFormatter.ofPattern("HH:mm")
private val tabDate = DateTimeFormatter.ofPattern("M/d E", Locale.JAPAN)
private val headingDate = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.JAPAN)
private val shortDateTime = DateTimeFormatter.ofPattern("M/d HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    item: TripWithLegs?,
    settings: UserSettings,
    onBack: () -> Unit,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
    onAddLeg: () -> Unit,
    onEditLeg: (Long) -> Unit,
    onMoveLeg: (Long, Int) -> Unit,
    onToday: () -> Unit,
    onPlans: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val trip = item?.trip
    val dates = trip?.let { start -> generateSequence(start.startDate) { day -> day.plusDays(1).takeIf { !it.isAfter(start.endDate) } }.toList() }.orEmpty()
    val initialDay = trip?.let { if (LocalDate.now() in it.startDate..it.endDate) LocalDate.now() else it.startDate }
    var selectedEpochDay by rememberSaveable(trip?.id) { mutableStateOf(initialDay?.toEpochDay()) }
    val selectedDate = selectedEpochDay?.let(LocalDate::ofEpochDay) ?: initialDay
    val dayLegs = item?.legs.orEmpty().filter { it.departureLocal.toLocalDate() == selectedDate }.sortedBy { it.sortOrder }
    val warningThreshold = settings.otherMinutes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "タイムライン", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 48.dp)) },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.SwapVert, "並べ替えと旅行メニュー") }
                        DropdownMenu(menuOpen, { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("旅行を編集") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menuOpen = false; onEditTrip() })
                            DropdownMenuItem(text = { Text("旅行を削除") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menuOpen = false; confirmDelete = true })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        bottomBar = {
            AppBottomBar(AppDestination.TIMELINE) { destination ->
                when (destination) {
                    AppDestination.TODAY -> onToday()
                    AppDestination.TIMELINE -> Unit
                    AppDestination.PLANS -> onPlans()
                }
            }
        },
        floatingActionButton = {
            if (dayLegs.isNotEmpty()) FloatingActionButton(
                onClick = onAddLeg,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Rounded.Add, "移動を追加") }
        },
    ) { padding ->
        if (item == null || selectedDate == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("読み込み中…") }
        else Column(Modifier.fillMaxSize().padding(padding)) {
            val selectedTabIndex = dates.indexOf(selectedDate).coerceAtLeast(0)
            if (dates.size < 6) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                    dates.forEach { date ->
                        Tab(
                            selected = date == selectedDate,
                            onClick = { selectedEpochDay = date.toEpochDay() },
                            text = { Text(date.format(tabDate), maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            } else {
                PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                    dates.forEach { date ->
                        Tab(
                            selected = date == selectedDate,
                            onClick = { selectedEpochDay = date.toEpochDay() },
                            text = { Text(date.format(tabDate), maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }
            if (dayLegs.isEmpty()) EmptyDay(onAddLeg)
            else LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
            ) {
                item {
                    Text(selectedDate.format(headingDate), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp, start = 8.dp))
                    Text(
                        "移動 ${dayLegs.size}件 ・ ${dayLegs.first().departureLocal.format(timelineTime)} – ${dayLegs.last().arrivalLocal.format(timelineTime)}",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 16.dp),
                    )
                }
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
                            val minutes = Duration.between(leg.arrival, dayLegs[index + 1].departure).toMinutes()
                            TimelineGap(minutes, warningThreshold)
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text("旅行を削除しますか？") },
        text = { Text("この旅行に含まれる移動もすべて削除されます。") },
        confirmButton = { TextButton(onClick = onDeleteTrip) { Text("削除") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") } },
    )
}

@Composable
private fun TimelineLeg(
    leg: TransportLeg,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
) {
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
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val title = leg.departurePlatform.ifBlank { "${leg.departurePlace} → ${leg.arrivalPlace}" }
                    Text(
                        title,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val type = if (leg.mode == TransportMode.TRAIN) leg.trainType?.label ?: leg.mode.detailLabel else leg.mode.detailLabel
                    val detail = if (leg.departurePlatform.isBlank()) type else "${leg.departurePlace} → ${leg.arrivalPlace} ・ $type"
                    Text(detail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun TimelineGap(minutes: Long, threshold: Int) {
    val warning = minutes < 0 || minutes < threshold
    Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(62.dp))
        Box(Modifier.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(4.dp).fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(2.dp)))
        }
        Box(Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (warning) Icons.Rounded.Warning else Icons.Rounded.Schedule, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    val label = if (minutes < 0) "時刻が ${-minutes}分 重複" else if (warning) "待ち ${minutes}分 ・ 乗り継ぎに注意" else "待ち ${minutes}分"
                    Text(label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

@Composable
private fun EmptyDay(onAddLeg: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("この日の移動はありません", style = MaterialTheme.typography.headlineSmall)
            FloatingActionButton(onClick = onAddLeg, modifier = Modifier.padding(top = 16.dp)) { Icon(Icons.Rounded.Add, "移動を追加") }
        }
    }
}
