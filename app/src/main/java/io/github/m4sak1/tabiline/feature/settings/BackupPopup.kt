package io.github.m4sak1.tabiline.feature.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.m4sak1.tabiline.data.backup.BackupDocument
import io.github.m4sak1.tabiline.data.backup.BackupManager
import io.github.m4sak1.tabiline.ui.components.CenterPopup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private class BackupViewModel(private val manager: BackupManager) : ViewModel() {
    var busy by mutableStateOf(false); private set
    var document by mutableStateOf<BackupDocument?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var failed by mutableStateOf(false); private set
    var lastExport by mutableStateOf(manager.lastExportAt); private set
    var restoreSettings by mutableStateOf(true)
    var acknowledged by mutableStateOf(false)
    var restored by mutableStateOf(false)

    private fun runOperation(action: suspend () -> String) {
        if (busy) return
        busy = true; message = null; failed = false
        viewModelScope.launch {
            try { message = withContext(Dispatchers.IO) { action() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true; message = "処理できませんでした。ファイルの内容・保存先の権限・空き容量を確認してください。" }
            finally { busy = false }
        }
    }
    fun export(uri: Uri) = runOperation {
        val result = manager.export(uri)
        withContext(Dispatchers.Main) { lastExport = result }
        "バックアップを保存しました。"
    }
    fun inspect(uri: Uri) {
        document = null; acknowledged = false; restoreSettings = true
        runOperation {
            val result = manager.inspect(uri)
            withContext(Dispatchers.Main) { document = result }
            "ファイルを確認しました。まだデータは変更していません。"
        }
    }
    fun clearPreview() { document = null; acknowledged = false; message = null }
    fun restore() {
        val preview = document ?: return
        if (!acknowledged) return
        val includeSettings = restoreSettings
        runOperation {
            manager.restore(preview, includeSettings)
            withContext(Dispatchers.Main) { document = null; acknowledged = false; restored = true }
            "復元しました。通知の権限は必要に応じて端末の設定から許可してください。"
        }
    }
}

@Composable
fun BackupPopup(manager: BackupManager, onDismiss: () -> Unit, onProgress: (Float) -> Unit, onRestored: () -> Unit) {
    val state: BackupViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BackupViewModel(manager) as T
    })
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { state.acknowledged = false }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        it?.let(state::export)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(state::inspect) }
    LaunchedEffect(state.restored) { if (state.restored) { onRestored(); state.restored = false } }
    BackHandler { if (!state.busy) visible = false }
    CenterPopup(visible = visible, onProgress = onProgress, onHidden = onDismiss) { _, motion ->
        Box(Modifier.fillMaxSize().clickable { if (!state.busy) visible = false })
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = motion.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = maxHeight)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("バックアップ", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                        IconButton(enabled = !state.busy, onClick = { visible = false }) { Icon(Icons.Rounded.Close, "閉じる") }
                    }
                    Text("旅行・移動・空き時間と設定を、1つのファイルに保存できます。")
                    Text("最終保存：${state.lastExport?.displayTime() ?: "まだ保存していません"}", style = MaterialTheme.typography.bodyMedium)
                    FilledTonalButton(enabled = !state.busy, onClick = {
                        save.launch("Tabiline-backup-${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))}.tabiline")
                    }, modifier = Modifier.fillMaxWidth()) { Text("現在のデータを保存") }
                    OutlinedButton(enabled = !state.busy, onClick = { open.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("ファイルから復元") }
                    Text("ファイルは暗号化されません。メモや予約情報を含むため、安全な場所に保管してください。通知の許可とアプリアイコンの色は復元対象外です。", style = MaterialTheme.typography.bodySmall)
                    state.document?.let { document ->
                        HorizontalDivider()
                        Text("復元するバックアップ", style = MaterialTheme.typography.titleMedium)
                        Text("作成：${document.createdAt.displayTime()}\n旅行 ${document.trips.size}件・予定 ${document.legCount}件")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.restoreSettings, enabled = !state.busy, onCheckedChange = { state.restoreSettings = it })
                            Text("設定も復元する")
                        }
                        BackupConfirmation(state.acknowledged, state.busy, { state.acknowledged = it }, state::restore)
                        TextButton(enabled = !state.busy, onClick = state::clearPreview) { Text("復元をやめる") }
                    }
                    if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    state.message?.let { Text(it, color = if (state.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) }
                }
            }
        }
    }
}

private fun Instant.displayTime() = atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))

@Composable
internal fun BackupConfirmation(acknowledged: Boolean, busy: Boolean, onAcknowledge: (Boolean) -> Unit, onRestore: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("現在のデータはすべて上書きされます", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text("事前にバックアップしていない既存データは、復元後に取り戻せません。旅行・移動・空き時間はすべて置き換わります。", color = MaterialTheme.colorScheme.onErrorContainer)
                Text("(；・_・)", color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = acknowledged, enabled = !busy, onCheckedChange = onAcknowledge)
            Text("既存データを取り戻せないことを理解しました", style = MaterialTheme.typography.bodyMedium)
        }
        Button(enabled = !busy && acknowledged, onClick = onRestore,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) {
            Text("現在のデータを上書きして復元")
        }
    }
}
