package io.github.m4sak1.tabiline.ui.components

import androidx.compose.foundation.layout.*
import android.os.Build
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePopup(date: LocalDate, onDismiss: () -> Unit, onDate: (LocalDate) -> Unit) {
    // Material's calendar uses UTC dates, independently of the journey's timezone.
    val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    PickerPopup("日付を選択", onDismiss, state.selectedDateMillis != null, {
        state.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
    }) {
        DatePicker(state = state, title = null, headline = null, showModeToggle = false,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePopup(time: LocalTime, onDismiss: () -> Unit, onTime: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
    val dialScale = remember { Animatable(1f) }
    LaunchedEffect(state.selection) {
        dialScale.snapTo(0.97f)
        dialScale.animateTo(1f, tween(180))
    }
    PickerPopup("時刻を選択", onDismiss, true, { onTime(LocalTime.of(state.hour, state.minute)) }) {
        TimePicker(state = state, modifier = Modifier.align(Alignment.CenterHorizontally).graphicsLayer {
            scaleX = dialScale.value
            scaleY = dialScale.value
        },
            layoutType = TimePickerLayoutType.Vertical)
    }
}

@Composable
private fun PickerPopup(
    title: String,
    onDismiss: () -> Unit,
    canConfirm: Boolean,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    var confirmed by remember { mutableStateOf(false) }
    val latestConfirm by rememberUpdatedState(onConfirm)
    val behind = LocalView.current.rootView
    DisposableEffect(behind) {
        onDispose { if (Build.VERSION.SDK_INT >= 31) behind.setRenderEffect(null) }
    }
    Dialog(onDismissRequest = { visible = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val popupWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            popupWindow?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        CenterPopup(visible = visible, onHidden = {
            if (confirmed) latestConfirm()
            onDismiss()
        }) { progress, motion ->
            SideEffect {
                if (Build.VERSION.SDK_INT >= 31) {
                    val radius = 20f * behind.resources.displayMetrics.density * progress
                    behind.setRenderEffect(if (radius > 0f) RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP) else null)
                }
            }
            Box(Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClick = { visible = false },
            ))
            Surface(
                modifier = motion.align(Alignment.Center).padding(horizontal = 16.dp, vertical = 24.dp).widthIn(max = 400.dp).fillMaxWidth(),
                shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = { visible = false }) { Icon(Icons.Rounded.Close, "閉じる") }
                    }
                    Spacer(Modifier.height(12.dp))
                    content()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        TextButton(onClick = { visible = false }, enabled = visible) { Text("キャンセル") }
                        Button(onClick = { confirmed = true; visible = false }, enabled = visible && canConfirm) { Text("決定") }
                    }
                }
            }
        }
    }
}
