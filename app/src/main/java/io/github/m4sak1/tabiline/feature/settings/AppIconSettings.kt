package io.github.m4sak1.tabiline.feature.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.R
import io.github.m4sak1.tabiline.ui.components.*

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
internal fun AppIconSettings() {
    val context = LocalContext.current
    val current = remember { AppIconChoice.current(context) }
    var pending by remember { mutableStateOf<AppIconChoice?>(null) }
    var error by remember { mutableStateOf(false) }
    Text("アイコンと起動アニメーションの色を選べます。変更するとアプリを自動で開き直します。ホーム画面への反映には少し時間がかかる場合があります。")
    AppIconChoice.entries.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { choice ->
                Surface(onClick = { if (choice != current) pending = choice }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = if (choice == current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = choice.color, shape = RoundedCornerShape(16.dp)) {
                            Icon(painterResource(R.drawable.ic_notification), null, Modifier.size(52.dp), tint = Color.White)
                        }
                        Text(choice.title, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
    if (error) Text("変更できませんでした。もう一度お試しください。", color = MaterialTheme.colorScheme.error)
    pending?.let { choice ->
        AppAlertDialog(onDismissRequest = { pending = null }, title = { Text("アイコンを変更しますか？") },
            text = { Text("${choice.title}に変更して、アプリを自動で開き直します。") },
            confirmButton = { close -> TextButton(onClick = { close {
                pending = null
                val activity = context.activity()
                error = activity == null || runCatching { changeAppIcon(activity, choice) }.isFailure
            }
            }) { Text("変更して再起動") } },
            dismissButton = { close -> TextButton(onClick = { close { pending = null } }) { Text("キャンセル") } })
    }
}
