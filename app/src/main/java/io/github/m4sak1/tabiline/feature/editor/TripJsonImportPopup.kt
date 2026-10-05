package io.github.m4sak1.tabiline.feature.editor

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.WindowManager
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.ui.components.CenterPopup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

@Composable
internal fun TripJsonImportPopup(onDismiss: () -> Unit, pasteMode: Boolean, onApply: (TripWithLegs) -> Unit) {
    JsonImportPopup(onDismiss, pasteMode, TripJsonCodec.MAX_BYTES, "1MB", TripJsonCodec::decode, onApply)
}

@Composable
private fun <T> JsonImportPopup(onDismiss: () -> Unit, pasteMode: Boolean, maxBytes: Int, sizeLabel: String, decode: (String) -> T, onApply: (T) -> Unit) {
    var visible by remember { mutableStateOf(true) }
    var source by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var reading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<T?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestApply by rememberUpdatedState(onApply)
    val behind = LocalView.current.rootView
    DisposableEffect(behind) { onDispose { if (Build.VERSION.SDK_INT >= 31) behind.setRenderEffect(null) } }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            reading = true; error = null; source = ""
            scope.launch {
                try {
                    source = withContext(Dispatchers.IO) {
                        val bytes = ByteArrayOutputStream()
                        (context.contentResolver.openInputStream(uri) ?: error("Cannot open JSON")).use { input ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(bytes.size() + count <= maxBytes) { "JSONが大きすぎます（最大${sizeLabel}）。" }
                                bytes.write(buffer, 0, count)
                            }
                        }
                        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString()
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "ファイルを読み込めませんでした。UTF-8のJSON（最大${sizeLabel}）を選んでください。" }
                finally { reading = false }
            }
        }
        else if (!pasteMode && source.isBlank()) visible = false
    }
    LaunchedEffect(Unit) { if (!pasteMode) file.launch(arrayOf("*/*")) }
    Dialog(onDismissRequest = { if (!reading) visible = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) }
        CenterPopup(visible = visible, onHidden = { result?.let(latestApply); onDismiss() }) { progress, motion ->
            SideEffect {
                if (Build.VERSION.SDK_INT >= 31) {
                    val radius = 20f * behind.resources.displayMetrics.density * progress
                    behind.setRenderEffect(if (radius > 0f) RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP) else null)
                }
            }
            Box(Modifier.fillMaxSize().clickable { if (!reading) visible = false })
            BoxWithConstraints(Modifier.fillMaxSize().imePadding().systemBarsPadding().padding(20.dp), contentAlignment = Alignment.Center) {
                Surface(modifier = motion.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = maxHeight),
                    shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (pasteMode) "JSONをコピペで読み込む" else "JSONを読み込む", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                            IconButton(onClick = { visible = false }, enabled = !reading) { Icon(Icons.Rounded.Close, "JSONを閉じる") }
                        }
                        Text(if (pasteMode) "AIから受け取ったJSONを貼り付けてください。1行にまとまったJSONも読み込めます。" else "AIが作成したJSONファイルを端末から選んで読み込みます。外部サーバーには送信しません。")
                        if (!pasteMode) FilledTonalButton(onClick = { file.launch(arrayOf("*/*")) }, enabled = !reading) { Text("JSONファイルを選ぶ") }
                        if (pasteMode) FilledTonalButton(onClick = {
                            val copied = runCatching {
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                    .primaryClip?.let { if (it.itemCount > 0) it.getItemAt(0).text?.toString() else null }
                            }.getOrNull()
                            when {
                                copied.isNullOrBlank() -> error = "クリップボードに文字列がありません。JSONをコピーしてからお試しください。"
                                copied.toByteArray(Charsets.UTF_8).size > maxBytes -> error = "JSONが大きすぎます（最大${sizeLabel}）。"
                                else -> { source = copied; error = null }
                            }
                        }, enabled = !reading) { Text("クリップボードから貼り付け") }
                        if (pasteMode) OutlinedTextField(value = source, onValueChange = {
                            if (it.length <= maxBytes) { source = it; error = null }
                            else error = "JSONが大きすぎます（最大${sizeLabel}）。"
                        }, enabled = !reading, label = { Text("JSONを貼り付け") }, minLines = 5, maxLines = 10,
                            shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth())
                        else if (source.isNotBlank()) Text("ファイルを読み込みました。入力欄へ反映して確認できます。")
                        Text("現在の入力内容を置き換えます。この操作だけでは登録されません。反映後に内容を確認して保存してください。", style = MaterialTheme.typography.bodyMedium)
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (reading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                        Button(enabled = !reading && source.isNotBlank() && visible, onClick = {
                            reading = true; error = null
                            scope.launch {
                                try { result = withContext(Dispatchers.Default) { decode(source) }; visible = false }
                                catch (cancelled: CancellationException) { throw cancelled }
                                catch (failure: IllegalArgumentException) { error = failure.message }
                                finally { reading = false }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("入力欄へ反映") }
                    }
                }
            }
        }
    }
}
