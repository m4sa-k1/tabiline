package io.github.m4sak1.tabiline.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.ui.components.AppBottomBar
import io.github.m4sak1.tabiline.ui.components.AppDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val planDateFormat = DateTimeFormatter.ofPattern("M/d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    trips: List<TripWithLegs>,
    selectedTripId: Long?,
    onCreateTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    onToday: () -> Unit,
    onTimeline: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    val today = LocalDate.now()
    val ongoing = trips.filter { today in it.trip.startDate..it.trip.endDate }
    val upcoming = trips.filter { it.trip.startDate > today }
    val completed = trips.filter { it.trip.endDate < today }.sortedByDescending { it.trip.endDate }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tabiline") },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "設定") } },
            )
        },
        bottomBar = {
            AppBottomBar(AppDestination.PLANS) { destination ->
                when (destination) {
                    AppDestination.TODAY -> onToday()
                    AppDestination.TIMELINE -> selectedTripId?.let(onTimeline)
                    AppDestination.PLANS -> Unit
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTrip,
                icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("新しい旅行") },
            )
        },
    ) { padding ->
        if (trips.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Luggage, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("旅行はまだありません", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
                    ExtendedFloatingActionButton(onClick = onCreateTrip, modifier = Modifier.padding(top = 20.dp),
                        icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("新しい旅行") })
                }
            }
        } else LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
        ) {
            item { Text("旅行", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) }
            planSection("進行中", ongoing, onOpenTrip)
            planSection("これから", upcoming, onOpenTrip)
            planSection("終了した旅行", completed, onOpenTrip)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.planSection(
    title: String,
    plans: List<TripWithLegs>,
    onOpenTrip: (Long) -> Unit,
) {
    if (plans.isEmpty()) return
    item(key = "section-$title") {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp))
    }
    itemsIndexed(plans, key = { _, item -> item.trip.id }) { index, item ->
        val first = index == 0
        val last = index == plans.lastIndex
        Card(
            onClick = { onOpenTrip(item.trip.id) },
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(bottom = if (last) 12.dp else 3.dp),
            shape = RoundedCornerShape(
                topStart = if (first) 28.dp else 8.dp, topEnd = if (first) 28.dp else 8.dp,
                bottomStart = if (last) 28.dp else 8.dp, bottomEnd = if (last) 28.dp else 8.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Luggage, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(item.trip.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${item.trip.startDate.format(planDateFormat)} – ${item.trip.endDate.format(planDateFormat)} ・ 移動 ${item.legs.size}件",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
