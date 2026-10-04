package io.github.m4sak1.tabiline.feature.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.TrainType
import io.github.m4sak1.tabiline.core.model.GapType
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.core.model.Trip
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
    initialTripId: Long?,
    availableTrips: List<Trip>,
    existing: TransportLeg?,
    previous: TransportLeg?,
    defaultZoneId: String,
    isLoading: Boolean,
    isSaving: Boolean,
    saveError: String?,
    onBack: () -> Unit,
    onSave: (TransportLeg, Boolean) -> Unit,
    onDelete: ((Long) -> Unit)?,
) {
    if (isLoading) return
    val defaultZone = runCatching { ZoneId.of(defaultZoneId).id }.getOrDefault("Asia/Tokyo")
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
    val selectableTrips = remember(availableTrips) { availableTrips.filterNot { it.isAutomatic } }
    val existingTripIsAutomatic = availableTrips.firstOrNull { it.id == existing?.tripId }?.isAutomatic == true
    var selectedTripId by remember(existing?.id, initialTripId) {
        mutableStateOf(if (existingTripIsAutomatic) null else initialTripId)
    }

    val departureZoneId = runCatching { ZoneId.of(departureZone) }.getOrNull()
    val arrivalZoneId = runCatching { ZoneId.of(arrivalZone) }.getOrNull()
    val departureInstant = departureZoneId?.let { LocalDateTime.of(departureDate, departureTime).atZone(it).toInstant() }
    val arrivalInstant = arrivalZoneId?.let { LocalDateTime.of(arrivalDate, arrivalTime).atZone(it).toInstant() }
    val valid = departurePlace.isNotBlank() && arrivalPlace.isNotBlank() && departureInstant != null &&
        arrivalInstant != null && arrivalInstant > departureInstant
    val overlapsRegularTrip = selectedTripId == null && selectableTrips.any {
        !arrivalDate.isBefore(it.startDate) && !departureDate.isAfter(it.endDate)
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, "閉じる") }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(if (existing == null) "移動を追加" else "移動を編集", style = MaterialTheme.typography.headlineMedium)
                    Text("時刻と乗り場をまとめて登録", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (existing != null && onDelete != null) IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, "削除", tint = MaterialTheme.colorScheme.error)
                }
            }
            EditorSection("旅行") {
                Text("この移動をまとめる旅行を選択できます", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTripId == null,
                        onClick = { selectedTripId = null },
                        label = { Text("紐づけない") },
                    )
                    selectableTrips.forEach { trip ->
                        FilterChip(
                            selected = selectedTripId == trip.id,
                            onClick = { selectedTripId = trip.id },
                            label = { Text(trip.name) },
                        )
                    }
                }
                if (overlapsRegularTrip) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp)) {
                            Icon(Icons.Rounded.Warning, null, Modifier.size(20.dp))
                            Text(
                                "この日には旅行が設定されています。紐づけずに保存すると、旅行一覧に日付名で別表示されます。",
                                Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                }
            }
            EditorSection("交通手段") {
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
                        OutlinedTextField(
                            value = trainType.label, onValueChange = {}, readOnly = true,
                            label = { Text("電車の種別") },
                            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(expanded = trainTypeMenu, onDismissRequest = { trainTypeMenu = false }) {
                            TrainType.entries.forEach { value ->
                                DropdownMenuItem(text = { Text(value.label) }, onClick = { trainType = value; trainTypeMenu = false })
                            }
                        }
                    }
                }
            }
            EditorSection("出発") {
                EditorField(departurePlace, { departurePlace = it }, "出発地", { Icon(Icons.Rounded.TripOrigin, null) })
                DateTimeRow(departureDate, departureTime, { departureDate = it }, { departureTime = it })
                EditorField(departurePlatform, { departurePlatform = it }, "出発の乗り場", { Icon(Icons.Rounded.Signpost, null) }, "ホーム番号・ゲート・バースなど")
                EditorField(departureZone, { departureZone = it }, "出発地のタイムゾーン", isError = departureZoneId == null)
            }
            EditorSection("到着") {
                EditorField(arrivalPlace, { arrivalPlace = it }, "到着地", { Icon(Icons.Rounded.LocationOn, null) })
                DateTimeRow(arrivalDate, arrivalTime, { arrivalDate = it }, {
                    arrivalTime = it
                    if (arrivalDate == departureDate && !it.isAfter(departureTime)) arrivalDate = departureDate.plusDays(1)
                })
                EditorField(arrivalPlatform, { arrivalPlatform = it }, "到着の乗り場", { Icon(Icons.Rounded.Signpost, null) }, "ホーム番号・ゲート・バースなど")
                EditorField(arrivalZone, { arrivalZone = it }, "到着地のタイムゾーン", isError = arrivalZoneId == null)
            }
            EditorSection("詳細") {
                OutlinedTextField(
                    memo, { memo = it }, label = { Text("メモ") }, supportingText = { Text("列車名・便名、座席、予約番号など") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, null) }, shape = RoundedCornerShape(20.dp),
                    minLines = 2, modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!valid) Text(
                "出発地・到着地と有効なタイムゾーンを入力し、到着を出発より後にしてください。",
                color = MaterialTheme.colorScheme.error,
            )
            saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                enabled = valid && !isSaving,
                onClick = {
                    onSave(TransportLeg(
                        id = existing?.id ?: 0,
                        tripId = selectedTripId ?: existing?.tripId ?: 0,
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
                        precedingGapType = existing?.precedingGapType ?: GapType.WAIT,
                    ), selectedTripId == null)
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(22.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Rounded.Check, null)
                }
                Text(if (isSaving) "保存中…" else "保存", Modifier.padding(start = 8.dp))
            }
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
private fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    supporting: String? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        supportingText = supporting?.let { { Text(it) } },
        isError = isError,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
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
        Surface(
            onClick = { DatePickerDialog(context, { _, y, m, d -> onDate(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Schedule, null, Modifier.size(20.dp))
                Text("日付", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date.format(dateFormatter), style = MaterialTheme.typography.titleSmall)
            }
        }
        Surface(
            onClick = { TimePickerDialog(context, { _, h, m -> onTime(LocalTime.of(h, m)) }, time.hour, time.minute, true).show() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Schedule, null, Modifier.size(20.dp))
                Text("時刻", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(time.format(timeFormatter), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
