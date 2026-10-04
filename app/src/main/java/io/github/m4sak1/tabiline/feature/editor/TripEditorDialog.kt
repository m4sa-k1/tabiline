package io.github.m4sak1.tabiline.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.Trip
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd")

@Composable
fun TripEditorDialog(existing: Trip?, onDismiss: () -> Unit, onSave: (Trip) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var start by remember(existing) { mutableStateOf((existing?.startDate ?: LocalDate.now()).format(dateFormat)) }
    var end by remember(existing) { mutableStateOf((existing?.endDate ?: LocalDate.now().plusDays(1)).format(dateFormat)) }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    val startDate = runCatching { LocalDate.parse(start, dateFormat) }.getOrNull()
    val endDate = runCatching { LocalDate.parse(end, dateFormat) }.getOrNull()
    val valid = name.isNotBlank() && startDate != null && endDate != null && !endDate.isBefore(startDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "旅行を作成" else "旅行を編集") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("旅行名") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(start, { start = it }, label = { Text("開始日") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(end, { end = it }, label = { Text("終了日") }, modifier = Modifier.weight(1f).padding(start = 8.dp))
                }
                OutlinedTextField(note, { note = it }, label = { Text("メモ（任意）") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                if (!valid) Text("日付は yyyy/MM/dd 形式で、終了日は開始日以降にしてください。")
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave((existing ?: Trip(name = name, startDate = startDate!!, endDate = endDate!!)).copy(name = name.trim(), startDate = startDate!!, endDate = endDate!!, note = note.trim())) },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
