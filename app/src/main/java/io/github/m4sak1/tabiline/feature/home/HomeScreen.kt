package io.github.m4sak1.tabiline.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.ui.components.AppBottomBar
import io.github.m4sak1.tabiline.ui.components.AppDestination
import io.github.m4sak1.tabiline.ui.components.visual
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    trips: List<TripWithLegs>,
    onCreateTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    onEditLeg: (Long, Long) -> Unit,
    onAddLeg: (Long) -> Unit,
    onPlans: () -> Unit,
    onTimeline: (Long) -> Unit,
) {
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); clock = System.currentTimeMillis() } }
    val now = Instant.ofEpochMilli(clock)
    val today = LocalDate.now()
    val focus = trips.firstOrNull { today in it.trip.startDate..it.trip.endDate }
        ?: trips.filter { it.trip.startDate > today }.minByOrNull { it.trip.startDate }
    val isToday = focus?.let { today in it.trip.startDate..it.trip.endDate } == true
    val ordered = focus?.legs.orEmpty().sortedBy { it.departure }
    val eligible = if (isToday) ordered.filter {
        it.departureLocal.toLocalDate() == today && it.departure >= now
    } else ordered
    val hero = eligible.firstOrNull()
    val following = hero?.let { selected -> ordered.filter { it.departure > selected.departure }.take(3) }.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tabiline") },
                actions = { IconButton(onClick = onPlans) { Icon(Icons.Rounded.CalendarMonth, "旅行一覧") } },
            )
        },
        bottomBar = {
            AppBottomBar(AppDestination.TODAY) { destination ->
                when (destination) {
                    AppDestination.TODAY -> Unit
                    AppDestination.TIMELINE -> focus?.trip?.id?.let(onTimeline)
                    AppDestination.PLANS -> onPlans()
                }
            }
        },
        floatingActionButton = {
            if (hero != null && focus != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAddLeg(focus.trip.id) },
                    icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("移動を追加") },
                )
            }
        },
    ) { padding ->
        if (hero == null || focus == null) {
            EmptyToday(Modifier.padding(padding)) {
                if (focus == null) onCreateTrip() else onAddLeg(focus.trip.id)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    HeroCard(
                        leg = hero,
                        remaining = countdown(hero.departure, now),
                        onClick = { onOpenTrip(focus.trip.id) },
                    )
                }
                if (following.isNotEmpty()) {
                    item { Text("このあとの移動", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp)) }
                    itemsIndexed(following, key = { _, leg -> leg.id }) { index, leg ->
                        FutureLegRow(
                            leg = leg,
                            first = index == 0,
                            last = index == following.lastIndex,
                            onClick = { onEditLeg(focus.trip.id, leg.id) },
                        )
                    }
                    val wait = Duration.between(hero.arrival, following.first().departure).toMinutes()
                    if (wait in 0..<15) item { TightTransferChip(wait) }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(leg: TransportLeg, remaining: String, onClick: () -> Unit) {
    val visual = leg.mode.visual()
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(264.dp),
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(Modifier.padding(24.dp)) {
            Text("次の移動", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.TopStart))
            Text(remaining, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.TopEnd))
            Text(
                leg.departureLocal.format(timeFormat),
                fontSize = 72.sp, lineHeight = 76.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).size(96.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(visual.icon, leg.mode.label, Modifier.size(48.dp), tint = visual.color) }
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(
                    leg.departurePlatform.ifBlank { "乗り場未設定" },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text("${leg.departurePlace} → ${leg.arrivalPlace}", style = MaterialTheme.typography.titleMedium)
                val detail = buildList {
                    add(if (leg.mode == TransportMode.TRAIN) leg.trainType?.label ?: leg.mode.label else leg.mode.label)
                    if (leg.memo.isNotBlank()) add(leg.memo)
                }.joinToString(" ・ ")
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun FutureLegRow(leg: TransportLeg, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val visual = leg.mode.visual()
    val shape = RoundedCornerShape(
        topStart = if (first) 28.dp else 8.dp, topEnd = if (first) 28.dp else 8.dp,
        bottomStart = if (last) 28.dp else 8.dp, bottomEnd = if (last) 28.dp else 8.dp,
    )
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(visual.container, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Icon(visual.icon, null, tint = visual.color)
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("${leg.departureLocal.format(timeFormat)} → ${leg.arrivalLocal.format(timeFormat)}", style = MaterialTheme.typography.bodyLarge)
                val type = if (leg.mode == TransportMode.TRAIN) leg.trainType?.label ?: leg.mode.label else leg.mode.label
                val platform = leg.departurePlatform.takeIf(String::isNotBlank)
                Text(
                    listOfNotNull("${leg.departurePlace} → ${leg.arrivalPlace}", type, platform).joinToString(" ・ "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TightTransferChip(minutes: Long) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
            Text("次の乗り継ぎ 待ち ${minutes}分", Modifier.padding(start = 6.dp),
                color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyToday(modifier: Modifier, onAdd: () -> Unit) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(40.dp)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("今日の移動はありません", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("追加する") })
            }
        }
    }
}

private fun countdown(target: Instant, now: Instant): String {
    val minutes = Duration.between(now, target).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 60 -> "あと ${minutes}分"
        minutes < 24 * 60 -> "あと ${minutes / 60}時間${minutes % 60}分"
        else -> "あと ${minutes / 1440}日${minutes % 1440 / 60}時間"
    }
}
