package io.github.m4sak1.tabiline.feature.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val planDateFormat = DateTimeFormatter.ofPattern("M/d")

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PlansScreen(
    trips: List<TripWithLegs>,
    selectedTripId: Long?,
    onOpenTrip: (Long) -> Unit,
) {
    val today = LocalDate.now()
    val automatic = trips.filter { it.trip.isAutomatic }.sortedByDescending { it.trip.startDate }
    val regularTrips = trips.filterNot { it.trip.isAutomatic }
    val ongoing = regularTrips.filter { today in it.trip.startDate..it.trip.endDate }
    val upcoming = regularTrips.filter { it.trip.startDate > today }
    val completed = regularTrips.filter { it.trip.endDate < today }.sortedByDescending { it.trip.endDate }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        if (trips.isEmpty()) {
            Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "( ˶ᵔ ᵕ ᵔ˶ )",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "旅行はまだありません",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        "右下のボタンから新しい旅行を作れます",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        } else LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 144.dp),
        ) {
            item { Text("旅行", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp, start = 8.dp, bottom = 10.dp)) }
            planSection("進行中", ongoing, onOpenTrip)
            planSection("これから", upcoming, onOpenTrip)
            planSection("日付別の移動", automatic, onOpenTrip)
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
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            plans.forEachIndexed { index, item ->
                val first = index == 0
                val last = index == plans.lastIndex
                Card(
                    onClick = { onOpenTrip(item.trip.id) },
                    modifier = Modifier.fillMaxWidth().height(72.dp),
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
                if (!last) androidx.compose.foundation.layout.Spacer(Modifier.height(3.dp))
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
        }
    }
}
