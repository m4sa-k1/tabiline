package io.github.m4sak1.tabiline.feature.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
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
import io.github.m4sak1.tabiline.ui.components.CenterPopup

@Composable
internal fun AiPromptPopup(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(AiPlanStyle.DESTINATION) }
    var copied by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val behind = LocalView.current.rootView
    DisposableEffect(behind) { onDispose { if (Build.VERSION.SDK_INT >= 31) behind.setRenderEffect(null) } }
    Dialog(onDismissRequest = { visible = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) }
        CenterPopup(visible = visible, onHidden = onDismiss) { progress, motion ->
            SideEffect {
                if (Build.VERSION.SDK_INT >= 31) {
                    val radius = 20f * behind.resources.displayMetrics.density * progress
                    behind.setRenderEffect(if (radius > 0f) RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP) else null)
                }
            }
            Box(Modifier.fillMaxSize().clickable { visible = false })
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp), contentAlignment = Alignment.Center) {
                Surface(modifier = motion.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = maxHeight),
                    shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AIに考えてもらう", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                            IconButton(onClick = { visible = false }) { Icon(Icons.Rounded.Close, "AIプロンプトを閉じる") }
                        }
                        Text("目的に合うプロンプトをコピーし、お使いのAIに貼り付けて相談できます。アプリからAIへ自動送信することはありません。")
                        AiPlanStyle.entries.forEach { style ->
                            Surface(onClick = { selected = style; copied = false }, shape = RoundedCornerShape(20.dp),
                                color = if (selected == style) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selected == style, onClick = null)
                                    Column(Modifier.padding(start = 10.dp)) {
                                        Text(style.title, style = MaterialTheme.typography.titleSmall)
                                        Text(style.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("コピーした後に入力してください", style = MaterialTheme.typography.titleSmall)
                                Text("【要入力：…】を自分の出発地・年月日・予算・人数などに置き換えてからAIへ送ってください。【任意：…】は希望か「なし」「おまかせ」に置き換えます。現在地は自動取得されません。")
                            }
                        }
                        Button(onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Tabiline AIプロンプト", AiPromptTemplates.create(selected)))
                                copied = true
                                Toast.makeText(context, "コピーしました。【要入力：…】を自分の情報に置き換えてください。", Toast.LENGTH_LONG).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, "コピーできませんでした。もう一度お試しください。", Toast.LENGTH_LONG).show()
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("プロンプトをコピー")
                        }
                        if (copied) Text("コピーしました。【要入力：…】を入力してからAIへ送ってください。", color = MaterialTheme.colorScheme.primary)
                        Text("AIの回答は誤っていることがあります。運行時刻や乗り場は公式情報で確認してください。返されたJSONは1件ずつ、ファイルまたはコピペのメニューから取り込み、確認後に保存します。", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
