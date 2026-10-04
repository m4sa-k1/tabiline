package io.github.m4sak1.tabiline.feature.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.domain.LegMoment
import io.github.m4sak1.tabiline.core.domain.NextLegResolver
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.ui.components.LegCard
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    trips: List<TripWithLegs>,
    onCreateTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    var ticker by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); ticker = System.currentTimeMillis() } }
    val now = Instant.ofEpochMilli(ticker)
    val active = trips.firstOrNull { item ->
        val today = LocalDate.now()
        !today.isBefore(item.trip.startDate) && !today.isAfter(item.trip.endDate)
    } ?: trips.firstOrNull { it.trip.startDate >= LocalDate.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tabiline", style = MaterialTheme.typography.headlineMedium) },
                actions = { FilledIconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "設定") } },
            )
        },
        floatingActionButton = {
            if (trips.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onCreateTrip,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("旅行を作成") },
                )
            }
        },
    ) { padding ->
        if (trips.isEmpty()) EmptyHome(Modifier.padding(padding), onCreateTrip)
        else LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            active?.let { trip ->
                val highlight = NextLegResolver.resolve(trip.legs, now)
                item {
                    Column {
                        Text("今日の移動", style = MaterialTheme.typography.headlineLarge)
                        Text(trip.trip.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        if (highlight.leg != null) {
                            val label = when (highlight.moment) {
                                LegMoment.CURRENT -> "移動中"
                                LegMoment.NEXT -> countdown(highlight.leg.departure, now)
                                LegMoment.COMPLETE -> "本日の移動は完了"
                            }
                            Text(label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            LegCard(highlight.leg, true, { onOpenTrip(trip.trip.id) })
                        } else {
                            Card(onClick = { onOpenTrip(trip.trip.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("移動を追加して旅程を作りましょう", Modifier.padding(20.dp))
                            }
                        }
                    }
                }
            }
            item {
                Text("旅行一覧", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            }
            items(trips, key = { it.trip.id }) { item ->
                TripCard(item, onClick = { onOpenTrip(item.trip.id) })
            }
        }
    }
}

@Composable
private fun EmptyHome(modifier: Modifier, onCreateTrip: () -> Unit) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Luggage, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("最初の旅をつくろう", style = MaterialTheme.typography.headlineMedium)
            Text("電車も飛行機も、ひとつのタイムラインへ")
            Spacer(Modifier.height(20.dp))
            ExtendedFloatingActionButton(onClick = onCreateTrip) { Text("旅行を作成") }
        }
    }
}

@Composable
private fun TripCard(item: TripWithLegs, onClick: () -> Unit) {
    val format = DateTimeFormatter.ofPattern("M月d日")
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.trip.name, style = MaterialTheme.typography.titleLarge)
                Text("${item.trip.startDate.format(format)} 〜 ${item.trip.endDate.format(format)}")
            }
            Text("${item.legs.size}件", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun countdown(target: Instant, now: Instant): String {
    val minutes = Duration.between(now, target).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 60 -> "あと ${minutes}分"
        minutes < 24 * 60 -> "あと ${minutes / 60}時間${minutes % 60}分"
        else -> "あと ${minutes / (24 * 60)}日"
    }
}
