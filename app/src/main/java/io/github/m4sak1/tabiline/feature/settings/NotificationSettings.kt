package io.github.m4sak1.tabiline.feature.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.TabilineApplication

@Composable
internal fun NotificationSettings(settings: UserSettings, onUpdate: (UserSettings) -> Unit) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    var exact by remember { mutableStateOf(exactAllowed()) }
    val latestSettings by rememberUpdatedState(settings)
    val latestUpdate by rememberUpdatedState(onUpdate)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        latestUpdate(latestSettings.copy(notificationsEnabled = granted))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        exact = exactAllowed()
        (context.applicationContext as TabilineApplication).container.refreshReminders()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("出発前に通知", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked = settings.notificationsEnabled, onCheckedChange = { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= 33 && !allowed) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else onUpdate(settings.copy(notificationsEnabled = enabled))
        })
    }
    Text("交通手段ごとに出発の何分前に通知するか設定できます。空き時間は開始前に通知します。0分は出発・開始時刻です。")
    if (!allowed) {
        Text("端末側で通知が許可されていません。", color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("端末の通知設定を開く") }
    }
    if (!exact && Build.VERSION.SDK_INT >= 31) {
        Text("正確なアラームが未許可のため、通知が遅れる場合があります。")
        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }) { Text("正確なアラームを許可") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TransportMode.entries.forEach { mode ->
            ThresholdStepper(mode.label, settings.notificationMinutes[mode] ?: 10) {
                onUpdate(settings.copy(notificationMinutes = settings.notificationMinutes + (mode to it)))
            }
        }
    }
    Text("設定した通知時刻を過ぎている予定は通知しません。端末の省電力設定やアプリの強制停止で届かない場合があります。", style = MaterialTheme.typography.bodySmall)
}
