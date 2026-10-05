package io.github.m4sak1.tabiline.feature.editor

import io.github.m4sak1.tabiline.ui.components.AppDatePopup
import io.github.m4sak1.tabiline.ui.components.AppTimePopup
import androidx.activity.compose.BackHandler
import io.github.m4sak1.tabiline.ui.components.DiscardChangesDialog
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
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material.icons.rounded.Warning
import io.github.m4sak1.tabiline.ui.components.AppAlertDialog as AlertDialog
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
    val initialArrival = existing?.arrivalLocal?.toLocalDateTime()
        ?.takeIf { existing.mode != TransportMode.FREE_TIME || it.isAfter(initialDeparture) }
        ?: initialDeparture.plusHours(1)
    var departureDate by remember(existing?.id) { mutableStateOf(initialDeparture.toLocalDate()) }
    var departureTime by remember(existing?.id) { mutableStateOf(initialDeparture.toLocalTime()) }
    var arrivalDate by remember(existing?.id) { mutableStateOf(initialArrival.toLocalDate()) }
    var arrivalTime by remember(existing?.id) { mutableStateOf(initialArrival.toLocalTime()) }
    var departurePlace by remember(existing?.id) { mutableStateOf(existing?.departurePlace ?: previous?.arrivalPlace.orEmpty()) }
    var arrivalPlace by remember(existing?.id) { mutableStateOf(existing?.arrivalPlace.orEmpty()) }
    var departurePlatform by remember(existing?.id) { mutableStateOf(existing?.departurePlatform.orEmpty()) }
    var arrivalPlatform by remember(existing?.id) { mutableStateOf(existing?.arrivalPlatform.orEmpty()) }
    var departureTerminal by remember(existing?.id) { mutableStateOf(existing?.departureTerminal.orEmpty()) }
    var arrivalTerminal by remember(existing?.id) { mutableStateOf(existing?.arrivalTerminal.orEmpty()) }
    var boardingGroup by remember(existing?.id) { mutableStateOf(existing?.boardingGroup.orEmpty()) }
    var flightNumber by remember(existing?.id) { mutableStateOf(existing?.flightNumber.orEmpty()) }
    var departureZone by remember(existing?.id) { mutableStateOf(existing?.departureZoneId ?: previous?.arrivalZoneId ?: defaultZone) }
    var arrivalZone by remember(existing?.id) { mutableStateOf(existing?.arrivalZoneId ?: departureZone) }
    var mode by remember(existing?.id) {
        mutableStateOf(
            existing?.mode
                ?: previous?.mode?.takeUnless { it == TransportMode.FREE_TIME }
                ?: TransportMode.TRAIN,
        )
    }
    var trainType by remember(existing?.id) { mutableStateOf(existing?.trainType ?: previous?.trainType ?: TrainType.LOCAL) }
    var trainLine by remember(existing?.id) { mutableStateOf(existing?.trainLine ?: previous?.trainLine.orEmpty()) }
    var busLine by remember(existing?.id) { mutableStateOf(existing?.busLine ?: previous?.busLine.orEmpty()) }
    var busType by remember(existing?.id) { mutableStateOf(existing?.busType ?: io.github.m4sak1.tabiline.core.model.BusType.LOCAL) }
    var busTypeMenu by remember { mutableStateOf(false) }
    var trainTypeMenu by remember { mutableStateOf(false) }
    var memo by remember(existing?.id) { mutableStateOf(existing?.memo.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var precedingGapType by remember(existing?.id) { mutableStateOf(existing?.precedingGapType ?: GapType.WAIT) }
    val selectableTrips = remember(availableTrips) { availableTrips.filterNot { it.isAutomatic } }
    val existingTripIsAutomatic = availableTrips.firstOrNull { it.id == existing?.tripId }?.isAutomatic == true
    var selectedTripId by remember(existing?.id, initialTripId) {
        mutableStateOf(if (existingTripIsAutomatic) null else initialTripId)
    }

    val isFreeTime = mode == TransportMode.FREE_TIME
    val draft = listOf(departureDate, departureTime, arrivalDate, arrivalTime, departurePlace, arrivalPlace,
        departurePlatform, arrivalPlatform, departureTerminal, arrivalTerminal, boardingGroup, flightNumber,
        departureZone, arrivalZone, mode, trainType, trainLine, busLine, busType, memo, selectedTripId, precedingGapType)
    val initialDraft = remember(existing?.id) { draft }
    var confirmDiscard by remember { mutableStateOf(false) }
    fun requestClose() {
        if (!isSaving) {
            if (draft != initialDraft) confirmDiscard = true else onBack()
        }
    }
    BackHandler { requestClose() }
    if (confirmDiscard) DiscardChangesDialog(
        onKeepEditing = { confirmDiscard = false },
        onDiscard = { confirmDiscard = false; onBack() },
    )
    val departureZoneId = runCatching { ZoneId.of(departureZone) }.getOrNull()
    val arrivalZoneId = runCatching { ZoneId.of(arrivalZone) }.getOrNull()
    val departureInstant = departureZoneId?.let { LocalDateTime.of(departureDate, departureTime).atZone(it).toInstant() }
    val arrivalInstant = (if (isFreeTime) departureZoneId else arrivalZoneId)?.let {
        LocalDateTime.of(arrivalDate, arrivalTime).atZone(it).toInstant()
    }
    val valid = if (isFreeTime) {
        departureInstant != null && arrivalInstant != null && arrivalInstant > departureInstant
    } else {
        departurePlace.isNotBlank() && arrivalPlace.isNotBlank() && departureInstant != null &&
            arrivalInstant != null && arrivalInstant > departureInstant
    }
    val scheduleEndDate = arrivalDate
    val overlapsRegularTrip = selectedTripId == null && selectableTrips.any {
        !scheduleEndDate.isBefore(it.startDate) && !departureDate.isAfter(it.endDate)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick = ::requestClose, enabled = !isSaving) { Icon(Icons.Rounded.Close, "閉じる") }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        when {
                            isFreeTime && existing == null -> "空き時間を追加"
                            isFreeTime -> "空き時間を編集"
                            existing == null -> "移動を追加"
                            else -> "移動を編集"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        if (isFreeTime) "用事名と時刻を登録" else "時刻と乗り場をまとめて登録",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (existing != null && onDelete != null) IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, "削除", tint = MaterialTheme.colorScheme.error)
                }
            }
            EditorSection("旅行") {
                Text(
                    if (isFreeTime) "この予定をまとめる旅行を選択できます" else "この移動をまとめる旅行を選択できます",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                            selected = mode == value,
                            onClick = {
                                if (value == TransportMode.FREE_TIME && mode != TransportMode.FREE_TIME && existing?.mode != TransportMode.FREE_TIME) {
                                    departurePlace = ""
                                }
                                mode = value
                            },
                            label = { Text(value.label) }, leadingIcon = { Icon(visual.icon, null) },
                        )
                    }
                }
                if (mode == TransportMode.BUS) {
                    EditorField(busLine, { busLine = it }, "路線名（任意）",
                        { Icon(mode.visual().icon, null) }, "例：京都駅行き、東京・大阪線")
                    ExposedDropdownMenuBox(expanded = busTypeMenu, onExpandedChange = { busTypeMenu = it }) {
                        OutlinedTextField(value = busType.label, onValueChange = {}, readOnly = true,
                            label = { Text("バスの種別") },
                            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
                        ExposedDropdownMenu(expanded = busTypeMenu, onDismissRequest = { busTypeMenu = false }) {
                            io.github.m4sak1.tabiline.core.model.BusType.entries.forEach { value ->
                                DropdownMenuItem(text = { Text(value.label) }, onClick = { busType = value; busTypeMenu = false })
                            }
                        }
                    }
                }
                if (mode == TransportMode.TRAIN) {
                    EditorField(
                        trainLine,
                        { trainLine = it },
                        "路線名（任意）",
                        { Icon(Icons.Rounded.Train, null) },
                        "例：予讃線、山手線",
                    )
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
            if (mode == TransportMode.FLIGHT) {
                EditorSection("フライト情報") {
                    EditorField(flightNumber, { flightNumber = it }, "便番号", supporting = "例：NH540、JL123")
                    EditorField(boardingGroup, { boardingGroup = it }, "搭乗Group", supporting = "例：2、A")
                }
            }
            if (isFreeTime) {
                EditorSection("空き時間") {
                    EditorField(
                        departurePlace,
                        { departurePlace = it },
                        "用事名（任意）",
                        { Icon(Icons.AutoMirrored.Rounded.Notes, null) },
                        "未入力の場合は「空き時間」と表示されます",
                    )
                    Text("開始", style = MaterialTheme.typography.titleMedium)
                    DateTimeRow(departureDate, departureTime, { departureDate = it }, { departureTime = it })
                    Text("終了", style = MaterialTheme.typography.titleMedium)
                    DateTimeRow(arrivalDate, arrivalTime, { arrivalDate = it }, { arrivalTime = it })
                    EditorField(departureZone, { departureZone = it }, "タイムゾーン", isError = departureZoneId == null)
                }
            } else {
                EditorSection("出発") {
                    EditorField(departurePlace, { departurePlace = it }, "出発地", { Icon(Icons.Rounded.TripOrigin, null) })
                    DateTimeRow(departureDate, departureTime, { departureDate = it }, { departureTime = it })
                    if (mode == TransportMode.FLIGHT) EditorField(departureTerminal, { departureTerminal = it }, "出発ターミナル")
                    EditorField(departurePlatform, { departurePlatform = it }, if (mode == TransportMode.FLIGHT) "出発ゲート" else "出発の乗り場", { Icon(Icons.Rounded.Signpost, null) })
                    EditorField(departureZone, { departureZone = it }, "出発地のタイムゾーン", isError = departureZoneId == null)
                }
                EditorSection("到着") {
                    EditorField(arrivalPlace, { arrivalPlace = it }, "到着地", { Icon(Icons.Rounded.LocationOn, null) })
                    DateTimeRow(arrivalDate, arrivalTime, { arrivalDate = it }, { arrivalTime = it })
                    if (mode == TransportMode.FLIGHT) EditorField(arrivalTerminal, { arrivalTerminal = it }, "到着ターミナル")
                    EditorField(arrivalPlatform, { arrivalPlatform = it }, if (mode == TransportMode.FLIGHT) "到着ゲート" else "到着の乗り場", { Icon(Icons.Rounded.Signpost, null) })
                    EditorField(arrivalZone, { arrivalZone = it }, "到着地のタイムゾーン", isError = arrivalZoneId == null)
                }
                EditorSection("詳細") {
                    OutlinedTextField(
                        memo, { memo = it }, label = { Text("メモ") }, supportingText = { Text("列車名・便名、座席、予約番号など") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, null) }, shape = RoundedCornerShape(20.dp),
                        minLines = 2, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (isFreeTime) EditorSection("メモ") {
                EditorField(memo, { memo = it }, "メモ", { Icon(Icons.AutoMirrored.Rounded.Notes, null) })
            }
            EditorSection("前の予定との間") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GapType.entries.forEach { value ->
                        FilterChip(selected = precedingGapType == value, onClick = { precedingGapType = value }, label = { Text(value.label) })
                    }
                }
            }
            if (!valid) Text(
                if (isFreeTime) "有効なタイムゾーンを入力し、終了を開始より後にしてください。"
                else "出発地・到着地と有効なタイムゾーンを入力し、到着を出発より後にしてください。",
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
                        arrivalZoneId = if (isFreeTime) departureZone else arrivalZone,
                        departurePlace = departurePlace.trim(),
                        arrivalPlace = if (isFreeTime) "" else arrivalPlace.trim(),
                        mode = mode,
                        trainType = trainType.takeIf { mode == TransportMode.TRAIN },
                        trainLine = trainLine.trim().takeIf { mode == TransportMode.TRAIN }.orEmpty(),
                        busLine = busLine.trim().takeIf { mode == TransportMode.BUS }.orEmpty(),
                        busType = busType.takeIf { mode == TransportMode.BUS },
                        departureTerminal = departureTerminal.trim().takeIf { mode == TransportMode.FLIGHT }.orEmpty(),
                        arrivalTerminal = arrivalTerminal.trim().takeIf { mode == TransportMode.FLIGHT }.orEmpty(),
                        boardingGroup = boardingGroup.trim().takeIf { mode == TransportMode.FLIGHT }.orEmpty(),
                        flightNumber = flightNumber.trim().takeIf { mode == TransportMode.FLIGHT }.orEmpty(),
                        departurePlatform = if (isFreeTime) "" else departurePlatform.trim(),
                        arrivalPlatform = if (isFreeTime) "" else arrivalPlatform.trim(),
                        memo = memo.trim(),
                        sortOrder = existing?.sortOrder ?: 0,
                        precedingGapType = precedingGapType,
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
        confirmButton = { close -> TextButton(onClick = { close { onDelete(existing.id) } }) { Text("削除") } },
        dismissButton = { close -> TextButton(onClick = { close { confirmDelete = false } }) { Text("キャンセル") } },
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
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    if (pickingDate) AppDatePopup(date, { pickingDate = false }, onDate)
    if (pickingTime) AppTimePopup(time, { pickingTime = false }, onTime)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = { pickingDate = true },
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
            onClick = { pickingTime = true },
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
