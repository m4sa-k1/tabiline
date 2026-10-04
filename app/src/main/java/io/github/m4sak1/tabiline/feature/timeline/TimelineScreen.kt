package io.github.m4sak1.tabiline.feature.timeline

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.domain.NextLegResolver
import io.github.m4sak1.tabiline.core.domain.TransferPolicy
import io.github.m4sak1.tabiline.core.domain.TransferStatus
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.core.model.UserSettings
import io.github.m4sak1.tabiline.ui.components.LegCard
import java.time.Instant
import java.time.format.DateTimeFormatter

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
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val trip = item?.trip
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "旅行") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "戻る") } },
                actions = {
                    IconButton(onClick = onEditTrip) { Icon(Icons.Rounded.Edit, "旅行を編集") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "旅行を削除") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddLeg,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("移動を追加") },
            )
        },
    ) { padding ->
        if (item == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("読み込み中…")
        } else if (item.legs.isEmpty()) Box(
            Modifier.fillMaxSize().padding(padding).padding(28.dp), contentAlignment = Alignment.Center,
        ) {
            Text("まだ移動がありません\n＋ボタンから最初の移動を追加できます")
        } else {
            val highlightId = NextLegResolver.resolve(item.legs, Instant.now()).leg?.id
            val groups = item.legs.groupBy { it.departureLocal.toLocalDate() }
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
            ) {
                groups.forEach { (date, legs) ->
                    item(key = "date-$date") {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("M月d日 EEEE")),
                            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    legs.forEachIndexed { index, leg ->
                        item(key = leg.id) {
                            Column(Modifier.animateContentSize()) {
                                LegCard(
                                    leg = leg,
                                    highlighted = leg.id == highlightId,
                                    onClick = { onEditLeg(leg.id) },
                                    onMove = { onMoveLeg(leg.id, it) },
                                )
                                val absoluteIndex = item.legs.indexOfFirst { it.id == leg.id }
                                if (absoluteIndex < item.legs.lastIndex) {
                                    TransferIndicator(
                                        TransferPolicy.evaluate(leg, item.legs[absoluteIndex + 1], settings),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("旅行を削除しますか？") },
        text = { Text("この旅行に含まれる移動もすべて削除されます。") },
        confirmButton = { TextButton(onClick = onDeleteTrip) { Text("削除") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") } },
    )
}

@Composable
private fun TransferIndicator(status: TransferStatus) {
    val warning = status is TransferStatus.Tight || status is TransferStatus.Overlap
    val text = when (status) {
        is TransferStatus.Comfortable -> "乗り継ぎ ${status.minutes}分"
        is TransferStatus.Tight -> "乗り継ぎ ${status.minutes}分・目安${status.recommendedMinutes}分未満"
        is TransferStatus.Overlap -> "時刻が ${-status.minutes}分 重複しています"
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (warning) Icon(
            Icons.Rounded.WarningAmber, null,
            tint = if (status is TransferStatus.Overlap) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (warning) FontWeight.Bold else FontWeight.Normal,
            color = if (status is TransferStatus.Overlap) MaterialTheme.colorScheme.error
            else if (warning) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
