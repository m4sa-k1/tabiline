package io.github.m4sak1.tabiline.feature.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.ui.components.detailLabel
import io.github.m4sak1.tabiline.ui.components.serviceLabel
import io.github.m4sak1.tabiline.ui.components.departureBoardingLabel
import io.github.m4sak1.tabiline.ui.components.visual
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    trips: List<TripWithLegs>,
    onOpenTrip: (Long) -> Unit,
    onEditLeg: (Long, Long) -> Unit,
) {
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); clock = System.currentTimeMillis() } }
    val now = Instant.ofEpochMilli(clock)
    val today = LocalDate.now()
    val focus = trips.firstOrNull { !it.trip.isAutomatic && today in it.trip.startDate..it.trip.endDate }
        ?: trips.firstOrNull { it.trip.isAutomatic && today in it.trip.startDate..it.trip.endDate }
        ?: trips.filter { !it.trip.isAutomatic && it.trip.startDate > today }.minByOrNull { it.trip.startDate }
    val isToday = focus?.let { today in it.trip.startDate..it.trip.endDate } == true
    val ordered = focus?.legs.orEmpty().sortedBy { it.departure }
    val eligible = if (isToday) ordered.filter {
        it.departureLocal.toLocalDate() == today && it.departure >= now
    } else ordered
    val hero = eligible.firstOrNull()
    val following = hero?.let { selected -> ordered.filter { it.departure > selected.departure }.take(3) }.orEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        if (hero == null || focus == null) {
            EmptyToday(Modifier.fillMaxSize().statusBarsPadding())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp, end = 16.dp, bottom = 144.dp),
            ) {
                item {
                    HeroCard(
                        leg = hero,
                        remaining = countdown(hero.departure, now),
                        onClick = { onOpenTrip(focus.trip.id) },
                    )
                }
                if (following.isNotEmpty()) {
                    val tightWait = (listOf(hero) + following).zipWithNext()
                        .map { (before, after) -> Duration.between(before.arrival, after.departure).toMinutes() }
                        .firstOrNull { it in 0..<15 }
                    item {
                        Column(Modifier.padding(top = 16.dp)) {
                            Text(
                                "このあとの移動",
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                            )
                            following.forEachIndexed { index, leg ->
                                FutureLegRow(
                                    leg = leg,
                                    first = index == 0,
                                    last = index == following.lastIndex,
                                    onClick = { onEditLeg(focus.trip.id, leg.id) },
                                )
                                if (index < following.lastIndex) Spacer(Modifier.height(3.dp))
                            }
                            tightWait?.let { wait ->
                                TightTransferChip(wait, Modifier.padding(start = 8.dp, top = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(leg: TransportLeg, remaining: String, onClick: () -> Unit) {
    val visual = leg.mode.visual()
    val isFreeTime = leg.mode == TransportMode.FREE_TIME
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(264.dp),
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box {
            Text(
                if (isFreeTime) "次の予定" else "次の移動",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(24.dp, 24.dp),
            )
            Text(
                remaining,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 24.dp),
            )
            Text(
                leg.departureLocal.format(timeFormat),
                fontSize = 72.sp, lineHeight = 76.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(24.dp, 52.dp),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 52.dp, end = 24.dp)
                    .size(96.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) { Icon(visual.icon, leg.mode.label, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimary) }
            }
            Text(
                if (isFreeTime) leg.departurePlace.ifBlank { "空き時間" }
                else leg.departureBoardingLabel.ifBlank { "乗り場未設定" },
                fontSize = if (leg.mode == TransportMode.FLIGHT) 18.sp else 30.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(24.dp, 140.dp),
            )
            if (isFreeTime) {
                Text(
                    "終了 ${leg.arrivalLocal.format(timeFormat)}",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(24.dp, 186.dp),
                )
            } else Text(
                    "${leg.departurePlace} → ${leg.arrivalPlace}",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(24.dp, 186.dp),
                )
            val detail = buildList {
                add(leg.serviceLabel)
                if (leg.memo.isNotBlank()) add(leg.memo)
            }.joinToString(" ・ ")
            Text(
                detail,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.offset(24.dp, 216.dp).padding(end = 24.dp),
            )
        }
    }
}

@Composable
private fun FutureLegRow(leg: TransportLeg, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val visual = leg.mode.visual()
    val isFreeTime = leg.mode == TransportMode.FREE_TIME
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
                Text(
                    "${leg.departureLocal.format(timeFormat)} → ${leg.arrivalLocal.format(timeFormat)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                val type = leg.serviceLabel
                val platform = leg.departureBoardingLabel.takeIf(String::isNotBlank)
                Text(
                    if (isFreeTime) listOf(leg.departurePlace.ifBlank { "空き時間" }, type).joinToString(" ・ ")
                    else listOfNotNull("${leg.departurePlace} → ${leg.arrivalPlace}", type, platform).joinToString(" ・ "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TightTransferChip(minutes: Long, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("次の乗り継ぎ 待ち ${minutes}分", Modifier.padding(start = 6.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyToday(modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "( ˘͈ ᵕ ˘͈ )",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "今日の移動はありません",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                "次の予定まで、のんびり過ごしましょう",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

internal fun countdown(target: Instant, now: Instant): String {
    val seconds = Duration.between(now, target).seconds.coerceAtLeast(0)
    val minutes = Duration.between(now, target).toMinutes().coerceAtLeast(0)
    return when {
        seconds < 3600 -> "あと ${seconds / 60}分${(seconds % 60).toString().padStart(2, '0')}秒"
        minutes < 24 * 60 -> "あと ${minutes / 60}時間${minutes % 60}分"
        else -> "あと ${minutes / 1440}日${minutes % 1440 / 60}時間"
    }
}
