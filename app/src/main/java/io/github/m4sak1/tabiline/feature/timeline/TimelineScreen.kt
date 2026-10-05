package io.github.m4sak1.tabiline.feature.timeline

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Warning
import io.github.m4sak1.tabiline.ui.components.AppAlertDialog as AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import io.github.m4sak1.tabiline.ui.components.serviceLabel
import io.github.m4sak1.tabiline.ui.components.departureBoardingLabel
import io.github.m4sak1.tabiline.ui.components.CenterPopup
import io.github.m4sak1.tabiline.ui.components.visual
import java.time.Duration
import java.time.LocalDate
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EmptyTimelineScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Box(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "( ˘ω˘ )",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "この日の移動はありません",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    "移動を追加すると、ここに表示されます",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

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
    onEditGapType: (Long, GapType) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val trip = item?.trip
    val dates = trip?.let { start ->
        val endpoints = item?.legs.orEmpty().flatMap { listOf(it.departureLocal.toLocalDate(), it.arrivalLocal.toLocalDate()) }
        val first = minOf(start.startDate, endpoints.minOrNull() ?: start.startDate)
        val last = maxOf(start.endDate, endpoints.maxOrNull() ?: start.endDate)
        generateSequence(first) { day -> day.plusDays(1).takeIf { !it.isAfter(last) } }.toList()
    }.orEmpty()
    val initialDay = trip?.let { if (LocalDate.now() in it.startDate..it.endDate) LocalDate.now() else it.startDate }
    var selectedEpochDay by rememberSaveable(trip?.id) { mutableStateOf(initialDay?.toEpochDay()) }
    var showAll by rememberSaveable(trip?.id) { mutableStateOf(false) }
    val selectedDate = selectedEpochDay?.let(LocalDate::ofEpochDay) ?: initialDay
    val dayLegs = item?.legs.orEmpty().filter { showAll || (selectedDate != null && it.isVisibleOn(selectedDate)) }.sortedBy { it.sortOrder }
    val referenceDate = if (showAll) dates.firstOrNull() else selectedDate
    val wideTimes = dayLegs.any { it.departureLocal.toLocalDate() != referenceDate || it.arrivalLocal.toLocalDate() != referenceDate }
    val timeColumnWidth = if (wideTimes) 88.dp else 62.dp
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }
    val headerHaze = remember { HazeState() }
    val timelineScroll = rememberLazyListState()
    val surfaceColor = MaterialTheme.colorScheme.surface

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        if (item == null || selectedDate == null) Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) { Text("読み込み中…") }
        else Box(Modifier.fillMaxSize()) {
            // Capture only the scrolling list, never the foreground title or controls.
            // The blur stays full strength behind controls and fades below their edge.
            Box(Modifier.fillMaxWidth().height(headerHeight + 48.dp).zIndex(0.5f)
                .hazeEffect(headerHaze) {
                    alpha = if (dayLegs.isEmpty()) 0f else if (timelineScroll.firstVisibleItemIndex > 0) 1f else
                        (timelineScroll.firstVisibleItemScrollOffset / with(density) { 48.dp.toPx() }).coerceIn(0f, 1f)
                    backgroundColor = surfaceColor
                    blurRadius = 24.dp
                    noiseFactor = 0f
                    tints = emptyList()
                    fallbackTint = HazeTint(surfaceColor.copy(alpha = 0.85f))
                    val fadeStart = with(density) { (headerHeight - 8.dp).coerceAtLeast(0.dp).toPx() }
                    val fadeEnd = with(density) { (headerHeight + 48.dp).toPx() }
                    mask = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = fadeStart,
                        endY = fadeEnd,
                    )
                })
            // The header floats above the full-window scrolling viewport.
            // Its height is initial content padding, never a clipping boundary.
            Column(Modifier.fillMaxWidth().zIndex(1f)
                .onSizeChanged { headerHeight = with(density) { it.height.toDp() } }
                .statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 36.dp, top = 24.dp, end = 24.dp, bottom = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.trip.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
                        ) { Icon(Icons.Rounded.MoreVert, "旅行メニュー") }
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
                if (dates.size > 1) RelativeDaySelector(dates, if (showAll) null else selectedDate) {
                    if (it == null && !showAll) {
                        // Reset on the same remeasure as the data change, rather than
                        // retaining the selected day's first visible item by its key.
                        timelineScroll.requestScrollToItem(0)
                    }
                    showAll = it == null
                    if (it != null) selectedEpochDay = it.toEpochDay()
                }
                if (showAll) Text("時刻は1日目を基準に表示", modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp), style = MaterialTheme.typography.labelSmall)
            }
                if (dayLegs.isNotEmpty()) {
                    LazyColumn(
                        state = timelineScroll,
                        modifier = Modifier.fillMaxSize().hazeSource(headerHaze),
                        contentPadding = PaddingValues(start = 16.dp, top = headerHeight + 12.dp, end = 16.dp, bottom = 144.dp),
                    ) {
                        dayLegs.forEachIndexed { index, leg ->
                            item(key = "leg-${leg.id}") {
                                TimelineLeg(
                                    leg = leg,
                                    referenceDate = referenceDate ?: selectedDate,
                                    timeColumnWidth = timeColumnWidth,
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
                                        timeColumnWidth = timeColumnWidth,
                                        minutes = minutes,
                                        threshold = settings.thresholdFor(next.mode),
                                        type = next.precedingGapType,
                                        onClick = { onEditGapType(next.id, next.precedingGapType) },
                                    )
                                }
                            }
                        }
                    }
                } else EmptyDay(Modifier.fillMaxSize().padding(top = headerHeight))
        }
    }
    if (confirmDelete && item != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(if (item.trip.isAutomatic) "この日の移動を削除しますか？" else "旅行を削除しますか？") },
        text = { Text(if (item.trip.isAutomatic) "この日付にまとめられた移動がすべて削除されます。" else "この旅行に含まれる移動もすべて削除されます。") },
        confirmButton = { close -> TextButton(onClick = { close(onDeleteTrip) }) { Text("削除") } },
        dismissButton = { close -> TextButton(onClick = { close { confirmDelete = false } }) { Text("キャンセル") } },
    )
}

@Composable
private fun RelativeDaySelector(
    dates: List<LocalDate>,
    selectedDate: LocalDate?,
    onSelect: (LocalDate?) -> Unit,
) {
    val choices: List<LocalDate?> = dates + listOf(null)
    val selectorWidth = (choices.size * 72 + (choices.size - 1) * 4).coerceAtMost(360).dp
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        LazyRow(
            modifier = Modifier.width(selectorWidth),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(choices, key = { _, date -> date?.toEpochDay() ?: Long.MIN_VALUE }) { index, date ->
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
                    targetValue = if (selected || index == choices.lastIndex) 22.dp else 5.dp,
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
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (date == null) "すべて" else "${index + 1}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineLeg(
    leg: TransportLeg,
    referenceDate: LocalDate,
    timeColumnWidth: androidx.compose.ui.unit.Dp,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val visual = leg.mode.visual()
    val isFreeTime = leg.mode == TransportMode.FREE_TIME
    var drag by remember { mutableFloatStateOf(0f) }
    Row(
        Modifier.fillMaxWidth().height(104.dp).zIndex(1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(timeColumnWidth).fillMaxHeight()) {
            Text(
                relativeTimelineTime(leg.departureLocal.toLocalDateTime(), referenceDate),
                fontSize = if (leg.departureLocal.toLocalDate() == referenceDate) 20.sp else 14.sp,
                maxLines = 1,
                softWrap = false,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart),
            )
            val crossesDate = leg.arrivalLocal.toLocalDate() != referenceDate
            Text(
                relativeTimelineTime(leg.arrivalLocal.toLocalDateTime(), referenceDate),
                fontSize = if (crossesDate) 14.sp else 16.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.align(Alignment.BottomStart),
                maxLines = 1,
                softWrap = false,
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
                    if (isFreeTime) {
                        Text(
                            leg.departurePlace.ifBlank { "空き時間" },
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "空き時間",
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val route = "${leg.departurePlace} → ${leg.arrivalPlace}"
                        val hasPlatform = leg.departureBoardingLabel.isNotBlank()
                        val title = leg.departureBoardingLabel.ifBlank { route }
                        Text(
                            title,
                            fontSize = if (leg.mode == TransportMode.FLIGHT) 14.sp else if (hasPlatform) 18.sp else 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = if (leg.mode == TransportMode.FLIGHT) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val type = leg.serviceLabel
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
    timeColumnWidth: androidx.compose.ui.unit.Dp,
    minutes: Long,
    threshold: Int,
    type: GapType,
    onClick: () -> Unit,
) {
    val warning = minutes < 0 || minutes < threshold
    val label = when {
        minutes < 0 -> "時刻が ${-minutes}分 重複"
        warning -> "${type.label} ${minutes}分 ・ 乗り継ぎに注意"
        else -> "${type.label} ${minutes}分"
    }
    Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(timeColumnWidth))
        Box(Modifier.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            val lineColor = MaterialTheme.colorScheme.outlineVariant
            Canvas(Modifier.width(4.dp).fillMaxHeight()) {
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 8.dp.toPx()),
                    ),
                )
            }
        }
        Box(Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
            Surface(
                onClick = onClick,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (warning) Icons.Rounded.Warning else type.icon(), null, Modifier.size(18.dp))
                    Text(
                        label,
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun GapTypePopup(
    selected: GapType,
    onSelect: (GapType) -> Unit,
    onDismiss: () -> Unit,
    onProgress: (Float) -> Unit = {},
) {
    var popupVisible by remember(selected) { mutableStateOf(true) }
    var closing by remember(selected) { mutableStateOf(false) }

    fun closeAfterMotion() {
        if (closing) return
        closing = true
        popupVisible = false
    }

    BackHandler(enabled = popupVisible && !closing) { closeAfterMotion() }

    CenterPopup(
        visible = popupVisible,
        onProgress = onProgress,
        onHidden = onDismiss,
    ) { _, motionModifier ->
        val outsideInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = outsideInteraction,
                indication = null,
                enabled = !closing,
                onClick = ::closeAfterMotion,
            ),
            contentAlignment = Alignment.Center,
        ) {
            val popupInteraction = remember { MutableInteractionSource() }
            Surface(
                modifier = motionModifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clickable(interactionSource = popupInteraction, indication = null) {},
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.SyncAlt, null, Modifier.size(26.dp))
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text("間の過ごし方", style = MaterialTheme.typography.headlineSmall)
                            Text("次の移動までの時間", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = ::closeAfterMotion, enabled = !closing) {
                            Icon(Icons.Rounded.Close, "閉じる")
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GapType.entries.forEach { choice ->
                            Surface(
                                onClick = {
                                    if (!closing) {
                                        onSelect(choice)
                                        closeAfterMotion()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = if (choice == selected) MaterialTheme.colorScheme.primaryContainer
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
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun GapType.icon() = when (this) {
    GapType.WAIT -> Icons.Rounded.Schedule
    GapType.TRANSFER -> Icons.Rounded.SyncAlt
}

@Composable
private fun EmptyDay(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "( ｡•ᴗ•｡ )",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "この日の移動はありません",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                "別の日を選ぶか、移動を追加してください",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
