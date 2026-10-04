package io.github.m4sak1.tabiline.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.BuildConfig
import io.github.m4sak1.tabiline.core.model.AccentPalette
import io.github.m4sak1.tabiline.core.model.ThemePreference
import io.github.m4sak1.tabiline.core.model.UserSettings
import io.github.m4sak1.tabiline.ui.components.AppBottomBar
import io.github.m4sak1.tabiline.ui.components.AppDestination
import io.github.m4sak1.tabiline.ui.theme.accentColors

private val footerFaces = listOf(
    "(·_·)", "(≥o≤)", "(;-;)", "(^-^*)", "(o^^)o",
    "(•‿•)", "(･ω･)", "(≧▽≦)", "(¬‿¬)", "(•̀ᴗ•́)و",
    "(╹▽╹)", "(ᵕ—ᴗ—)", "(｡•́︿•̀｡)",
)

private data class ZoneChoice(val label: String, val zoneId: String)

private val defaultZones = listOf(
    ZoneChoice("東京", "Asia/Tokyo"),
    ZoneChoice("ソウル", "Asia/Seoul"),
    ZoneChoice("シンガポール", "Asia/Singapore"),
    ZoneChoice("シドニー", "Australia/Sydney"),
    ZoneChoice("ロンドン", "Europe/London"),
    ZoneChoice("パリ", "Europe/Paris"),
    ZoneChoice("ニューヨーク", "America/New_York"),
    ZoneChoice("ロサンゼルス", "America/Los_Angeles"),
    ZoneChoice("ホノルル", "Pacific/Honolulu"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    var footerFace by rememberSaveable { mutableStateOf(footerFaces.random()) }
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = Color.Transparent,
        bottomBar = {
            AppBottomBar(
                selected = AppDestination.SETTINGS,
                onAdd = {
                    footerFace = footerFaces.filterNot { it == footerFace }.random()
                },
                addContentDescription = "表情を変える",
                addText = footerFace,
                onSelect = onDestination,
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("設定", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp))
            SettingsCard(title = "テーマ", subtitle = "アプリの見た目を選択") {
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
            }
            SettingsCard(title = "アクセントカラー", subtitle = "選択状態や追加ボタンの色") {
                AccentPalette.entries.forEach { palette ->
                    AccentChoice(
                        palette = palette,
                        selected = settings.accentPalette == palette,
                        onClick = { onUpdate(settings.copy(accentPalette = palette)) },
                    )
                }
            }
            SettingsCard(title = "デフォルト地域", subtitle = "新しい移動のタイムゾーンに使用") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    defaultZones.forEach { zone ->
                        FilterChip(
                            selected = settings.defaultZoneId == zone.zoneId,
                            onClick = { onUpdate(settings.copy(defaultZoneId = zone.zoneId)) },
                            label = { Text(zone.label) },
                        )
                    }
                }
                Text(
                    defaultZones.firstOrNull { it.zoneId == settings.defaultZoneId }?.zoneId ?: settings.defaultZoneId,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsCard(title = "乗り継ぎ警告", subtitle = "この時間を下回る乗り継ぎを警告します") {
                ThresholdStepper("電車", settings.trainMinutes) { onUpdate(settings.copy(trainMinutes = it)) }
                ThresholdStepper("バス", settings.busMinutes) { onUpdate(settings.copy(busMinutes = it)) }
                ThresholdStepper("飛行機", settings.flightMinutes) { onUpdate(settings.copy(flightMinutes = it)) }
                ThresholdStepper("船", settings.ferryMinutes) { onUpdate(settings.copy(ferryMinutes = it)) }
                ThresholdStepper("その他", settings.otherMinutes) { onUpdate(settings.copy(otherMinutes = it)) }
            }
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tabiline", style = MaterialTheme.typography.titleLarge)
                    Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge)
                    Text("旅の移動を、ひとつのタイムラインに。", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Composable
private fun AccentChoice(palette: AccentPalette, selected: Boolean, onClick: () -> Unit) {
    val colors = accentColors(palette)
    val label = when (palette) {
        AccentPalette.PURPLE -> "Purple"
        AccentPalette.ORCHID -> "Orchid"
        AccentPalette.BLUE -> "Blue"
        AccentPalette.GREEN -> "Green"
        AccentPalette.CORAL -> "Coral"
        AccentPalette.AMBER -> "Amber"
        AccentPalette.TEAL -> "Teal"
        AccentPalette.MONO -> "Mono"
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(colors.soft)
                    .then(if (selected) Modifier.border(3.dp, colors.strong, CircleShape) else Modifier),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawPath(
                        Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(0f, size.height)
                            close()
                        },
                        color = colors.strong,
                    )
                }
            }
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 14.dp))
            if (selected) Icon(Icons.Rounded.Check, "選択中", tint = colors.strong)
        }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun ThresholdStepper(label: String, value: Int, onValue: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { onValue((value - 5).coerceAtLeast(0)) }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Rounded.Remove, "5分減らす")
            }
            Box(Modifier.size(width = 64.dp, height = 42.dp), contentAlignment = Alignment.Center) {
                Text("${value}分", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onValue((value + 5).coerceAtMost(999)) }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Rounded.Add, "5分増やす")
            }
        }
    }
}
