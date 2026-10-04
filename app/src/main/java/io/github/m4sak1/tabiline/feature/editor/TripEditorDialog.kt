package io.github.m4sak1.tabiline.feature.editor

import android.app.DatePickerDialog
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import io.github.m4sak1.tabiline.core.model.Trip
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd")

@Composable
fun TripEditorDialog(existing: Trip?, onDismiss: () -> Unit, onSave: (Trip) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var start by remember(existing) { mutableStateOf(existing?.startDate ?: LocalDate.now()) }
    var end by remember(existing) { mutableStateOf(existing?.endDate ?: LocalDate.now().plusDays(1)) }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    val valid = name.isNotBlank() && !end.isBefore(start)

    Dialog(onDismissRequest = onDismiss) {
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setWindowAnimations(0) }
        val scale = remember { Animatable(0.12f) }
        LaunchedEffect(Unit) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        Surface(
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                transformOrigin = TransformOrigin(0.82f, 0.92f)
            },
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
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
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "閉じる") }
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
                    TextButton(onClick = onDismiss) { Text("キャンセル") }
                    Button(
                        enabled = valid,
                        onClick = {
                            onSave((existing ?: Trip(name = name, startDate = start, endDate = end)).copy(
                                name = name.trim(), startDate = start, endDate = end, note = note.trim(),
                            ))
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
