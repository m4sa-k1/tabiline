package io.github.m4sak1.tabiline.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import java.time.format.DateTimeFormatter

/** Shared read-only entry point; editing and deletion keep the existing persistence paths. */
@Composable
fun LegDetailPopup(
    leg: TransportLeg,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onProgress: (Float) -> Unit,
) {
    var visible by remember(leg.id) { mutableStateOf(true) }
    var confirmDelete by remember(leg.id) { mutableStateOf(false) }
    var afterClose by remember(leg.id) { mutableStateOf<(() -> Unit)?>(null) }
    fun close(action: (() -> Unit)? = null) {
        if (!visible) return
        afterClose = action
        visible = false
    }
    BackHandler {
        if (confirmDelete) confirmDelete = false else close()
    }
    CenterPopup(
        visible = visible,
        onProgress = onProgress,
        onHidden = { onDismiss(); afterClose?.invoke() },
    ) { _, motionModifier ->
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { close() },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = motionModifier.systemBarsPadding().padding(20.dp)
                    .widthIn(max = 520.dp).fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(leg.mode.visual().icon, null, Modifier.padding(14.dp).size(28.dp))
                        }
                        Text(if (confirmDelete) "削除の確認" else "移動の詳細", Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { close() }) { Icon(Icons.Rounded.Close, "閉じる") }
                    }
                    val freeTime = leg.mode == TransportMode.FREE_TIME
                    Text(
                        if (freeTime) leg.departurePlace.ifBlank { "空き時間" } else "${leg.departurePlace} → ${leg.arrivalPlace}",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    if (confirmDelete) {
                        Text("この移動を削除します。この操作は取り消せません。")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                            TextButton(onClick = { confirmDelete = false }, enabled = visible) { Text("キャンセル") }
                            Button(onClick = { close(onDelete) }, enabled = visible, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("削除する") }
                        }
                    } else {
                        Text(leg.serviceLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        val formatter = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm")
                        DetailField(if (freeTime) "開始" else "出発", leg.departureLocal.format(formatter))
                        val flight = leg.mode == TransportMode.FLIGHT
                        if (flight && leg.flightNumber.isNotBlank()) DetailField("便番号", leg.flightNumber)
                        if (flight && leg.boardingGroup.isNotBlank()) DetailField("搭乗Group", leg.boardingGroup)
                        if (flight && leg.departureTerminal.isNotBlank()) DetailField("出発ターミナル", leg.departureTerminal)
                        if (!freeTime && leg.departurePlatform.isNotBlank()) DetailField(if (flight) "出発ゲート" else "出発の乗り場", leg.departurePlatform)
                        DetailField(if (freeTime) "終了" else "到着", leg.arrivalLocal.format(formatter))
                        if (flight && leg.arrivalTerminal.isNotBlank()) DetailField("到着ターミナル", leg.arrivalTerminal)
                        if (!freeTime && leg.arrivalPlatform.isNotBlank()) DetailField(if (flight) "到着ゲート" else "到着の乗り場", leg.arrivalPlatform)
                        DetailField("タイムゾーン", if (leg.departureZoneId == leg.arrivalZoneId) leg.departureZoneId else "${leg.departureZoneId} → ${leg.arrivalZoneId}")
                        if (leg.memo.isNotBlank()) DetailField("メモ", leg.memo)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { confirmDelete = true }, enabled = visible, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp))
                                Text("削除", Modifier.padding(start = 8.dp))
                            }
                            Button(onClick = { close(onEdit) }, enabled = visible, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp))
                                Text("編集", Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
