package io.github.m4sak1.tabiline.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.BuildConfig
import io.github.m4sak1.tabiline.core.model.ThemePreference
import io.github.m4sak1.tabiline.core.model.UserSettings
import io.github.m4sak1.tabiline.ui.components.AppBottomBar
import io.github.m4sak1.tabiline.ui.components.AppDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        bottomBar = { AppBottomBar(AppDestination.SETTINGS, onSelect = onDestination) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("設定", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 4.dp))
            Text("テーマ", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference.entries.forEach { value ->
                    val label = when (value) {
                        ThemePreference.SYSTEM -> "端末設定"
                        ThemePreference.LIGHT -> "ライト"
                        ThemePreference.DARK -> "ダーク"
                    }
                    FilterChip(
                        selected = settings.theme == value,
                        onClick = { onUpdate(settings.copy(theme = value)) },
                        label = { Text(label) },
                    )
                }
            }
            Text("乗り継ぎ警告", style = MaterialTheme.typography.titleLarge)
            Text("次の交通手段に必要な目安時間を下回ると警告します。駅や空港内の距離は考慮されません。")
            ThresholdField("電車", settings.trainMinutes) { onUpdate(settings.copy(trainMinutes = it)) }
            ThresholdField("バス", settings.busMinutes) { onUpdate(settings.copy(busMinutes = it)) }
            ThresholdField("飛行機", settings.flightMinutes) { onUpdate(settings.copy(flightMinutes = it)) }
            ThresholdField("船", settings.ferryMinutes) { onUpdate(settings.copy(ferryMinutes = it)) }
            ThresholdField("その他", settings.otherMinutes) { onUpdate(settings.copy(otherMinutes = it)) }
            Text("アプリ情報", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
            Text("Tabiline ${BuildConfig.VERSION_NAME}")
            Text("旅の移動を、ひとつのタイムラインに。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThresholdField(label: String, value: Int, onValue: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new.filter(Char::isDigit).take(3)
            text.toIntOrNull()?.coerceIn(0, 999)?.let(onValue)
        },
        label = { Text("$label（分）") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
