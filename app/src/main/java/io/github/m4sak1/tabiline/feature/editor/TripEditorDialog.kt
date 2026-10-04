package io.github.m4sak1.tabiline.feature.editor

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.Trip
import io.github.m4sak1.tabiline.ui.components.CenterPopup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd")

@Composable
fun TripEditorDialog(
    existing: Trip?,
    onDismiss: () -> Unit,
    onSave: (Trip) -> Unit,
    onProgress: (Float) -> Unit = {},
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var start by remember(existing) { mutableStateOf(existing?.startDate ?: LocalDate.now()) }
    var end by remember(existing) { mutableStateOf(existing?.endDate ?: LocalDate.now().plusDays(1)) }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    var dialogVisible by remember { mutableStateOf(true) }
    var closing by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<Trip?>(null) }
    val valid = name.isNotBlank() && !end.isBefore(start)

    fun closeAfterMotion(trip: Trip? = null) {
        if (closing) return
        pendingSave = trip
        closing = true
        dialogVisible = false
    }

    BackHandler(enabled = dialogVisible && !closing) { closeAfterMotion() }

    CenterPopup(
        visible = dialogVisible,
        onProgress = onProgress,
        onHidden = { pendingSave?.let(onSave) ?: onDismiss() },
    ) { _, motionModifier ->
        val outsideInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = outsideInteraction,
                indication = null,
                enabled = !closing,
                onClick = { closeAfterMotion() },
            ),
            contentAlignment = Alignment.Center,
        ) {
            val popupInteraction = remember { MutableInteractionSource() }
            Surface(
                modifier = motionModifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .clickable(interactionSource = popupInteraction, indication = null) {},
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Rounded.Luggage, null, Modifier.padding(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(if (existing == null) "新しい旅行" else "旅行を編集", style = MaterialTheme.typography.headlineSmall)
                            Text("旅の名前と期間", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { closeAfterMotion() }, enabled = !closing) {
                            Icon(Icons.Rounded.Close, "閉じる")
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("旅行名") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TripDateCard("開始", start, Modifier.weight(1f)) {
                            start = it
                            if (end.isBefore(it)) end = it
                        }
                        TripDateCard("終了", end, Modifier.weight(1f)) { end = it }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("メモ（任意）") },
                        minLines = 2,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!valid) Text("旅行名を入力し、終了日を開始日以降にしてください。", color = MaterialTheme.colorScheme.error)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                        TextButton(onClick = { closeAfterMotion() }, enabled = !closing) { Text("キャンセル") }
                        Button(
                            enabled = valid && !closing,
                            onClick = {
                                closeAfterMotion(
                                    (existing ?: Trip(name = name, startDate = start, endDate = end)).copy(
                                        name = name.trim(), startDate = start, endDate = end, note = note.trim(),
                                    ),
                                )
                            },
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(Icons.Rounded.Check, null)
                            Text("保存", Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripDateCard(label: String, date: LocalDate, modifier: Modifier, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.clickable {
            DatePickerDialog(
                context,
                { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
                date.year, date.monthValue - 1, date.dayOfMonth,
            ).show()
        },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.CalendarMonth, null)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date.format(dateFormat), style = MaterialTheme.typography.titleMedium)
        }
    }
}
