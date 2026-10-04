package io.github.m4sak1.tabiline.feature.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Signpost
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.TrainType
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.ui.components.visual
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegEditorScreen(
    tripId: Long,
    existing: TransportLeg?,
    previous: TransportLeg?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSave: (TransportLeg) -> Unit,
    onDelete: ((Long) -> Unit)?,
) {
    if (isLoading) return
    val defaultZone = ZoneId.systemDefault().id
    val initialDeparture = existing?.departureLocal?.toLocalDateTime()
        ?: previous?.arrivalLocal?.toLocalDateTime()?.plusMinutes(10)
        ?: LocalDateTime.now().plusHours(1).withMinute(0)
    val initialArrival = existing?.arrivalLocal?.toLocalDateTime() ?: initialDeparture.plusHours(1)
    var departureDate by remember(existing?.id) { mutableStateOf(initialDeparture.toLocalDate()) }
    var departureTime by remember(existing?.id) { mutableStateOf(initialDeparture.toLocalTime()) }
    var arrivalDate by remember(existing?.id) { mutableStateOf(initialArrival.toLocalDate()) }
    var arrivalTime by remember(existing?.id) { mutableStateOf(initialArrival.toLocalTime()) }
    var departurePlace by remember(existing?.id) { mutableStateOf(existing?.departurePlace ?: previous?.arrivalPlace.orEmpty()) }
    var arrivalPlace by remember(existing?.id) { mutableStateOf(existing?.arrivalPlace.orEmpty()) }
    var departurePlatform by remember(existing?.id) { mutableStateOf(existing?.departurePlatform.orEmpty()) }
    var arrivalPlatform by remember(existing?.id) { mutableStateOf(existing?.arrivalPlatform.orEmpty()) }
    var departureZone by remember(existing?.id) { mutableStateOf(existing?.departureZoneId ?: previous?.arrivalZoneId ?: defaultZone) }
    var arrivalZone by remember(existing?.id) { mutableStateOf(existing?.arrivalZoneId ?: departureZone) }
    var mode by remember(existing?.id) { mutableStateOf(existing?.mode ?: previous?.mode ?: TransportMode.TRAIN) }
    var trainType by remember(existing?.id) { mutableStateOf(existing?.trainType ?: previous?.trainType ?: TrainType.LOCAL) }
    var trainTypeMenu by remember { mutableStateOf(false) }
    var memo by remember(existing?.id) { mutableStateOf(existing?.memo.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val departureZoneId = runCatching { ZoneId.of(departureZone) }.getOrNull()
    val arrivalZoneId = runCatching { ZoneId.of(arrivalZone) }.getOrNull()
    val departureInstant = departureZoneId?.let { LocalDateTime.of(departureDate, departureTime).atZone(it).toInstant() }
    val arrivalInstant = arrivalZoneId?.let { LocalDateTime.of(arrivalDate, arrivalTime).atZone(it).toInstant() }
    val valid = departurePlace.isNotBlank() && arrivalPlace.isNotBlank() && departureInstant != null &&
        arrivalInstant != null && arrivalInstant > departureInstant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "移動を追加" else "移動を編集") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, "閉じる") } },
                actions = {
                    if (existing != null && onDelete != null) IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.Delete, "削除")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("交通手段", style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransportMode.entries.forEach { value ->
                    val visual = value.visual()
                    FilterChip(
                        selected = mode == value, onClick = { mode = value },
                        label = { Text(value.label) }, leadingIcon = { Icon(visual.icon, null) },
                    )
                }
            }
            if (mode == TransportMode.TRAIN) {
                ExposedDropdownMenuBox(expanded = trainTypeMenu, onExpandedChange = { trainTypeMenu = it }) {
                    TextField(
                        value = trainType.label, onValueChange = {}, readOnly = true,
                        label = { Text("電車の種別") },
                        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = trainTypeMenu, onDismissRequest = { trainTypeMenu = false }) {
                        TrainType.entries.forEach { value ->
                            DropdownMenuItem(text = { Text(value.label) }, onClick = { trainType = value; trainTypeMenu = false })
                        }
                    }
                }
            }
            Text("出発", style = MaterialTheme.typography.titleLarge)
            TextField(departurePlace, { departurePlace = it }, label = { Text("出発地") },
                leadingIcon = { Icon(Icons.Rounded.TripOrigin, null) }, modifier = Modifier.fillMaxWidth())
            DateTimeRow(departureDate, departureTime, { departureDate = it }, { departureTime = it })
            TextField(departurePlatform, { departurePlatform = it }, label = { Text("出発の乗り場") },
                supportingText = { Text("ホーム番号・ゲート・バースなど") }, leadingIcon = { Icon(Icons.Rounded.Signpost, null) }, modifier = Modifier.fillMaxWidth())
            TextField(departureZone, { departureZone = it }, label = { Text("出発地のタイムゾーン") }, isError = departureZoneId == null, modifier = Modifier.fillMaxWidth())

            Text("到着", style = MaterialTheme.typography.titleLarge)
            TextField(arrivalPlace, { arrivalPlace = it }, label = { Text("到着地") },
                leadingIcon = { Icon(Icons.Rounded.LocationOn, null) }, modifier = Modifier.fillMaxWidth())
            DateTimeRow(arrivalDate, arrivalTime, { arrivalDate = it }, {
                arrivalTime = it
                if (arrivalDate == departureDate && !it.isAfter(departureTime)) arrivalDate = departureDate.plusDays(1)
            })
            TextField(arrivalPlatform, { arrivalPlatform = it }, label = { Text("到着の乗り場") },
                supportingText = { Text("ホーム番号・ゲート・バースなど") }, leadingIcon = { Icon(Icons.Rounded.Signpost, null) }, modifier = Modifier.fillMaxWidth())
            TextField(arrivalZone, { arrivalZone = it }, label = { Text("到着地のタイムゾーン") }, isError = arrivalZoneId == null, modifier = Modifier.fillMaxWidth())

            TextField(
                memo, { memo = it }, label = { Text("メモ") }, supportingText = { Text("列車名・便名、座席、予約番号など") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, null) },
                minLines = 2, modifier = Modifier.fillMaxWidth(),
            )
            if (!valid) Text(
                "出発地・到着地と有効なタイムゾーンを入力し、到着を出発より後にしてください。",
                color = MaterialTheme.colorScheme.error,
            )
            Button(
                enabled = valid,
                onClick = {
                    onSave(TransportLeg(
                        id = existing?.id ?: 0,
                        tripId = tripId,
                        departure = departureInstant!!,
                        arrival = arrivalInstant!!,
                        departureZoneId = departureZone,
                        arrivalZoneId = arrivalZone,
                        departurePlace = departurePlace.trim(),
                        arrivalPlace = arrivalPlace.trim(),
                        mode = mode,
                        trainType = trainType.takeIf { mode == TransportMode.TRAIN },
                        departurePlatform = departurePlatform.trim(),
                        arrivalPlatform = arrivalPlatform.trim(),
                        memo = memo.trim(),
                        sortOrder = existing?.sortOrder ?: 0,
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Icon(Icons.Rounded.Check, null); Text("保存", Modifier.padding(start = 8.dp)) }
        }
    }
    if (confirmDelete && existing != null && onDelete != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("この移動を削除しますか？") },
        confirmButton = { TextButton(onClick = { onDelete(existing.id) }) { Text("削除") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") } },
    )
}

@Composable
private fun DateTimeRow(
    date: LocalDate,
    time: LocalTime,
    onDate: (LocalDate) -> Unit,
    onTime: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).clickable {
            DatePickerDialog(context, { _, y, m, d -> onDate(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }) {
            TextField(
                value = date.format(dateFormatter), onValueChange = {}, enabled = false,
                label = { Text("日付") }, leadingIcon = { Icon(Icons.Rounded.Schedule, null) }, modifier = Modifier.fillMaxWidth(),
                colors = dateTimeFieldColors(),
            )
        }
        Box(Modifier.weight(1f).clickable {
            TimePickerDialog(context, { _, h, m -> onTime(LocalTime.of(h, m)) }, time.hour, time.minute, true).show()
        }) {
            TextField(
                value = time.format(timeFormatter), onValueChange = {}, enabled = false,
                label = { Text("時刻") }, leadingIcon = { Icon(Icons.Rounded.Schedule, null) }, modifier = Modifier.fillMaxWidth(),
                colors = dateTimeFieldColors(),
            )
        }
    }
}

@Composable
private fun dateTimeFieldColors() = TextFieldDefaults.colors(
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
