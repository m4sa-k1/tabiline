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
import io.github.m4sak1.tabiline.ui.components.DiscardChangesDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiPromptPopup(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(AiPlanStyle.DESTINATION) }
    var copied by remember { mutableStateOf(false) }
    val values = remember { mutableStateMapOf<AiPromptField, String>() }
    var pace by remember { mutableStateOf("標準") }
    var transport by remember { mutableStateOf(setOf<String>()) }
    var details by remember { mutableStateOf(false) }
    var styleMenu by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val options = AiPromptOptions(values.toMap(), pace, transport)
    val prompt = AiPromptTemplates.create(selected, options)
    val context = LocalContext.current
    fun requestClose() {
        if (!copied && (values.values.any { it.isNotBlank() } || pace != "標準" || transport.isNotEmpty())) confirmDiscard = true
        else visible = false
    }
    val behind = LocalView.current.rootView
    DisposableEffect(behind) { onDispose { if (Build.VERSION.SDK_INT >= 31) behind.setRenderEffect(null) } }
    Dialog(onDismissRequest = ::requestClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) }
        CenterPopup(visible = visible, onHidden = onDismiss) { progress, motion ->
            SideEffect {
                if (Build.VERSION.SDK_INT >= 31) {
                    val radius = 20f * behind.resources.displayMetrics.density * progress
                    behind.setRenderEffect(if (radius > 0f) RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP) else null)
                }
            }
            Box(Modifier.fillMaxSize().clickable { requestClose() })
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(20.dp), contentAlignment = Alignment.Center) {
                Surface(modifier = motion.widthIn(max = 520.dp).fillMaxWidth().heightIn(max = maxHeight),
                    shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AIに考えてもらう", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                            IconButton(onClick = ::requestClose) { Icon(Icons.Rounded.Close, "AIプロンプトを閉じる") }
                        }
                        Text("目的に合うプロンプトをコピーし、お使いのAIに貼り付けて相談できます。アプリからAIへ自動送信することはありません。")
                        Text("旅全体を計画する${AiPlanStyle.entries.size}種類のプロンプト", style = MaterialTheme.typography.titleSmall)
                        Box {
                            FilledTonalButton(onClick = { styleMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(selected.title) }
                            DropdownMenu(expanded = styleMenu, onDismissRequest = { styleMenu = false }) {
                                AiPlanStyle.entries.forEach { style ->
                                    DropdownMenuItem(text = { Column { Text(style.title); Text(style.description, style = MaterialTheme.typography.bodySmall) } },
                                        onClick = { selected = style; copied = false; styleMenu = false })
                                }
                            }
                        }
                        Text("条件を入力（空欄のままコピーもできます）", style = MaterialTheme.typography.titleSmall)
                        val required = AiPromptTemplates.requiredFields(selected)
                        AiPromptField.entries.filter { it in required || details }.forEach { field ->
                            OutlinedTextField(value = values[field].orEmpty(), onValueChange = { values[field] = it.take(2000); copied = false },
                                label = { Text("${field.label}${if (field in required) "（要入力）" else "（任意）"}") },
                                placeholder = { Text(field.hint) }, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), maxLines = 3)
                        }
                        TextButton(onClick = { details = !details }) { Text(if (details) "詳細な条件を閉じる" else "詳細な条件をカスタマイズ") }
                        Text("旅のペース", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ゆったり", "標準", "充実").forEach { value ->
                                FilterChip(selected = pace == value, onClick = { pace = value; copied = false }, label = { Text(value) })
                            }
                        }
                        Text("交通手段（複数選択・未選択はおまかせ）", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("電車", "飛行機", "船", "バス", "徒歩", "その他").forEach { value ->
                                FilterChip(selected = value in transport, onClick = {
                                    transport = if (value in transport) transport - value else transport + value; copied = false
                                }, label = { Text(value) })
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
                                clipboard.setPrimaryClip(ClipData.newPlainText("Tabiline 旅行AIプロンプト", prompt))
                                copied = true
                                Toast.makeText(context, "コピーしました。【要入力：…】を自分の情報に置き換えてください。", Toast.LENGTH_LONG).show()
                            } catch (_: Exception) {
                                Toast.makeText(context, "コピーできませんでした。もう一度お試しください。", Toast.LENGTH_LONG).show()
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("プロンプトをコピー")
                        }
                        if (copied) Text("コピーしました。【要入力：…】を入力してからAIへ送ってください。", color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { preview = !preview }) { Text(if (preview) "プロンプトの確認を閉じる" else "プロンプトを確認") }
                        if (preview) androidx.compose.foundation.text.selection.SelectionContainer { Text(prompt, style = MaterialTheme.typography.bodySmall) }
                        Text("AIの回答は誤っていることがあります。公式情報で確認してください。旅行全体のJSONをファイルまたはコピペで取り込み、予定一覧を確認してから保存します。入力した条件はコピー内容に含まれるため、外部AIへ送る情報にご注意ください。", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (confirmDiscard) DiscardChangesDialog(
            onKeepEditing = { confirmDiscard = false },
            onDiscard = { confirmDiscard = false; visible = false })
    }
}
