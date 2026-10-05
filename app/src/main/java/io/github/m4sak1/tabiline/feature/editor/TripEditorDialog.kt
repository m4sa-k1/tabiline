package io.github.m4sak1.tabiline.feature.editor

import io.github.m4sak1.tabiline.ui.components.AppDatePopup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.Trip
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import io.github.m4sak1.tabiline.ui.components.CenterPopup
import io.github.m4sak1.tabiline.ui.components.DiscardChangesDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd")

@Composable
fun TripEditorDialog(
    existing: Trip?,
    onDismiss: () -> Unit,
    onSave: (Trip) -> Unit,
    onProgress: (Float) -> Unit = {},
    onSavePlan: (suspend (TripWithLegs) -> Long)? = null,
    onPlanSaved: (Long) -> Unit = {},
    initialPlan: TripWithLegs? = null,
) {
    var name by remember(existing, initialPlan) { mutableStateOf(existing?.name ?: initialPlan?.trip?.name.orEmpty()) }
    var start by remember(existing, initialPlan) { mutableStateOf(existing?.startDate ?: initialPlan?.trip?.startDate ?: LocalDate.now()) }
    var end by remember(existing, initialPlan) { mutableStateOf(existing?.endDate ?: initialPlan?.trip?.endDate ?: LocalDate.now().plusDays(1)) }
    var note by remember(existing, initialPlan) { mutableStateOf(existing?.note ?: initialPlan?.trip?.note.orEmpty()) }
    var dialogVisible by remember { mutableStateOf(true) }
    var closing by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<Trip?>(null) }
    var importedLegs by remember(initialPlan) { mutableStateOf(initialPlan?.legs.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var savedPlanId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val valid = name.isNotBlank() && !end.isBefore(start)
    val draft = listOf(name, start, end, note, importedLegs)
    val initialDraft = remember(existing, initialPlan) { draft }
    var confirmDiscard by remember { mutableStateOf(false) }
    var discardApproved by remember { mutableStateOf(false) }

    fun closeAfterMotion(trip: Trip? = null) {
        if (closing || saving) return
        if (trip == null && (draft != initialDraft || initialPlan != null) && !discardApproved) {
            confirmDiscard = true
            return
        }
        pendingSave = trip
        closing = true
        dialogVisible = false
    }

    BackHandler(enabled = dialogVisible && !closing) { closeAfterMotion() }
    if (confirmDiscard) DiscardChangesDialog(
        onKeepEditing = { confirmDiscard = false },
        onDiscard = { confirmDiscard = false; discardApproved = true; closeAfterMotion() },
    )

    CenterPopup(
        visible = dialogVisible,
        onProgress = onProgress,
        onHidden = { savedPlanId?.let(onPlanSaved) ?: pendingSave?.let(onSave) ?: onDismiss() },
    ) { _, motionModifier ->
        val outsideInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = outsideInteraction,
                indication = null,
                enabled = !closing && !saving,
                onClick = { closeAfterMotion() },
            ),
        )
        Box(Modifier.fillMaxSize().statusBarsPadding().imePadding(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = motionModifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .heightIn(max = 620.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Box {
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
                        IconButton(onClick = { closeAfterMotion() }, enabled = !closing && !saving) {
                            Icon(Icons.Rounded.Close, "閉じる")
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        enabled = !saving,
                        onValueChange = { name = it },
                        label = { Text("旅行名") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TripDateCard("開始", start, Modifier.weight(1f), enabled = !saving) {
                            start = it
                            if (end.isBefore(it)) end = it
                        }
                        TripDateCard("終了", end, Modifier.weight(1f), enabled = !saving) { end = it }
                    }
                    OutlinedTextField(
                        value = note,
                        enabled = !saving,
                        onValueChange = { note = it },
                        label = { Text("メモ（任意）") },
                        minLines = 2,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (importedLegs.isNotEmpty()) {
                        Text("取り込んだ予定 ${importedLegs.size}件", style = MaterialTheme.typography.titleMedium)
                        Text("保存するまで登録されません。時刻・乗り換えを確認してください。保存後に各予定を編集できます。", style = MaterialTheme.typography.bodySmall)
                        importedLegs.forEach { leg ->
                            Text("${leg.departureLocal.format(DateTimeFormatter.ofPattern("M/d HH:mm"))} → ${leg.arrivalLocal.format(DateTimeFormatter.ofPattern("M/d HH:mm"))}\n${leg.departurePlace.ifBlank { "空き時間" }}${if (leg.arrivalPlace.isBlank()) "" else " → ${leg.arrivalPlace}"} · ${leg.mode.label}", style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = { importedLegs = emptyList(); saveError = null }, enabled = !saving) { Text("取り込んだ予定を取り除く") }
                    }
                    saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (!valid) Text("旅行名を入力し、終了日を開始日以降にしてください。", color = MaterialTheme.colorScheme.error)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                        TextButton(onClick = { closeAfterMotion() }, enabled = !closing && !saving) { Text("キャンセル") }
                        Button(
                            enabled = valid && !closing && !saving,
                            onClick = {
                                val trip = (existing ?: Trip(name = name, startDate = start, endDate = end)).copy(
                                    name = name.trim(), startDate = start, endDate = end, note = note.trim())
                                if (importedLegs.isEmpty()) closeAfterMotion(trip)
                                else if (onSavePlan != null) {
                                    saving = true; saveError = null
                                    scope.launch {
                                        try {
                                            TripJsonCodec.validateRange(trip, importedLegs.map { it.departureLocal.toLocalDate() to it.arrivalLocal.toLocalDate() })
                                            savedPlanId = onSavePlan(TripWithLegs(trip, importedLegs))
                                            closing = true; dialogVisible = false
                                        } catch (cancelled: CancellationException) { throw cancelled }
                                        catch (failure: Exception) { saveError = failure.message ?: "保存できませんでした。もう一度お試しください。" }
                                        finally { saving = false }
                                    }
                                } else saveError = "旅行の一括保存を利用できません。"
                            },
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(Icons.Rounded.Check, null)
                            Text(if (saving) "保存中…" else "保存", Modifier.padding(start = 8.dp))
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun TripDateCard(label: String, date: LocalDate, modifier: Modifier, enabled: Boolean = true, onDate: (LocalDate) -> Unit) {
    var pickingDate by remember { mutableStateOf(false) }
    if (pickingDate) AppDatePopup(date, { pickingDate = false }, onDate)
    Surface(
        modifier = modifier.clickable(enabled = enabled) {
            pickingDate = true
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
