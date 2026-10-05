package io.github.m4sak1.tabiline.feature.settings

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import io.github.m4sak1.tabiline.R
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.BuildConfig
import io.github.m4sak1.tabiline.core.model.AccentPalette
import io.github.m4sak1.tabiline.core.model.ThemePreference
import io.github.m4sak1.tabiline.core.model.UserSettings
import io.github.m4sak1.tabiline.ui.components.CenterPopup
import io.github.m4sak1.tabiline.ui.theme.accentColors

enum class SettingsSection(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    BACKUP("backup", "バックアップ", "データをファイルに保存・復元", Icons.Rounded.Backup),
    ACCENT("accent", "アクセントカラー", "アプリの色を選択", Icons.Rounded.Palette),
    ICON("icon", "アプリアイコン", "アイコンと起動アニメーションの色", Icons.Rounded.Palette),
    REGION("region", "デフォルト地域", "新しい移動のタイムゾーン", Icons.Rounded.Language),
    TRANSFER("transfer", "乗り継ぎ警告", "交通手段ごとの警告時間", Icons.Rounded.SyncAlt),
    NOTIFICATIONS("notifications", "出発通知", "オン・オフと交通手段ごとの通知時間", Icons.Rounded.Notifications),
    ABOUT("about", "Tabilineについて", "バージョンとアプリ情報", Icons.Rounded.Info),
    ;

    companion object {
        fun fromRoute(route: String?): SettingsSection? = entries.firstOrNull { it.route == route }
    }
}

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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    onOpenSection: (SettingsSection) -> Unit,
) {
    SettingsScaffold {
        Text("設定", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        ThemeSelector(settings.theme) { onUpdate(settings.copy(theme = it)) }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                SettingsSection.entries.filterNot { it == SettingsSection.ABOUT }.forEach { section ->
                    SettingsRow(section = section, onClick = { onOpenSection(section) })
                }
            }
        }
        SupportCard()
        AboutCard()
    }
}

@Composable
fun SettingsDetailPopup(
    section: SettingsSection,
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    onDismiss: () -> Unit,
    onProgress: (Float) -> Unit = {},
) {
    var popupVisible by remember(section) { mutableStateOf(true) }
    var closing by remember(section) { mutableStateOf(false) }

    fun closeAfterMotion() {
        if (closing) return
        closing = true
        popupVisible = false
    }

    BackHandler(enabled = popupVisible && !closing) { closeAfterMotion() }

    CenterPopup(
        visible = popupVisible,
        onProgress = onProgress,
        onHidden = onDismiss,
    ) { _, motionModifier ->
        val outsideInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = outsideInteraction,
                indication = null,
                enabled = !closing,
                onClick = ::closeAfterMotion,
            ),
            contentAlignment = Alignment.Center,
        ) {
            val popupInteraction = remember { MutableInteractionSource() }
            Surface(
                modifier = motionModifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .heightIn(max = 660.dp)
                    .clickable(interactionSource = popupInteraction, indication = null) {},
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(section.icon, null, Modifier.size(26.dp)) }
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(section.title, style = MaterialTheme.typography.headlineSmall)
                            Text(section.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = ::closeAfterMotion, enabled = !closing) {
                            Icon(Icons.Rounded.Close, "閉じる")
                        }
                    }

                    when (section) {
                        SettingsSection.BACKUP -> Unit
                        SettingsSection.ICON -> AppIconSettings()
                        SettingsSection.ACCENT -> SettingsCard {
                            AccentPalette.entries.chunked(2).forEach { row ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    row.forEach { palette ->
                                        AccentChoice(
                                            palette = palette,
                                            selected = settings.accentPalette == palette,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onUpdate(settings.copy(accentPalette = palette)) },
                                        )
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        SettingsSection.REGION -> SettingsCard {
                            defaultZones.chunked(3).forEach { row ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    row.forEach { zone ->
                                        ZoneChoiceCard(
                                            zone = zone,
                                            selected = settings.defaultZoneId == zone.zoneId,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onUpdate(settings.copy(defaultZoneId = zone.zoneId)) },
                                        )
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            Text(
                                defaultZones.firstOrNull { it.zoneId == settings.defaultZoneId }?.zoneId
                                    ?: settings.defaultZoneId,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        SettingsSection.TRANSFER -> SettingsCard {
                            ThresholdStepper("電車", settings.trainMinutes) { onUpdate(settings.copy(trainMinutes = it)) }
                            ThresholdStepper("バス", settings.busMinutes) { onUpdate(settings.copy(busMinutes = it)) }
                            ThresholdStepper("飛行機", settings.flightMinutes) { onUpdate(settings.copy(flightMinutes = it)) }
                            ThresholdStepper("船", settings.ferryMinutes) { onUpdate(settings.copy(ferryMinutes = it)) }
                            ThresholdStepper("その他", settings.otherMinutes) { onUpdate(settings.copy(otherMinutes = it)) }
                        }

                        SettingsSection.NOTIFICATIONS -> NotificationSettings(settings, onUpdate)
                        SettingsSection.ABOUT -> AboutCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportCard() {
    val uriHandler = LocalUriHandler.current
    var openFailed by remember { mutableStateOf(false) }
    Surface(
        onClick = {
            openFailed = runCatching { uriHandler.openUri("https://ko-fi.com/m4sak1") }.isFailure
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.kofi_logo), contentDescription = "Ko-fi", modifier = Modifier.size(36.dp))
                Text("開発者を応援する", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            }
            Text("Ko-fiで任意の支援ができます。支援の有無でアプリの機能は変わりません。")
            Text("ブラウザで開く ↗", style = MaterialTheme.typography.labelLarge)
            if (openFailed) Text("ブラウザを開けませんでした。https://ko-fi.com/m4sak1 をブラウザで開いてください。")
        }
    }
}

@Composable
private fun AboutCard() {
    val uriHandler = LocalUriHandler.current
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Tabiline", style = MaterialTheme.typography.headlineLarge)
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge)
            Text(
                "旅の移動を、ひとつのタイムラインに。",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            TextButton(onClick = { uriHandler.openUri("https://github.com/m4sa-k1/tabiline") }) {
                Column(Modifier.fillMaxWidth()) {
                    Text("GitHub", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("github.com/m4sa-k1/tabiline", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            TextButton(onClick = { uriHandler.openUri("https://m4sak1.me") }) {
                Column(Modifier.fillMaxWidth()) {
                    Text("開発者 · @m4sa-k1", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("https://m4sak1.me", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun ThemeSelector(selected: ThemePreference, onSelect: (ThemePreference) -> Unit) {
    val options = listOf(
        Triple(ThemePreference.SYSTEM, "端末設定", Icons.Rounded.SettingsBrightness),
        Triple(ThemePreference.LIGHT, "ライト", Icons.Rounded.LightMode),
        Triple(ThemePreference.DARK, "ダーク", Icons.Rounded.DarkMode),
    )
    val motion = tween<androidx.compose.ui.unit.Dp>(260, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                Text("テーマ", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 10.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEachIndexed { index, (value, label, icon) ->
                    val active = selected == value
                    val startRadius by animateDpAsState(
                        if (active || index == 0) 24.dp else 6.dp,
                        motion,
                        label = "theme-start-shape",
                    )
                    val endRadius by animateDpAsState(
                        if (active || index == options.lastIndex) 24.dp else 6.dp,
                        motion,
                        label = "theme-end-shape",
                    )
                    val container by animateColorAsState(
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        tween(260),
                        label = "theme-container",
                    )
                    val content by animateColorAsState(
                        if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        tween(260),
                        label = "theme-content",
                    )
                    Surface(
                        onClick = { onSelect(value) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(
                            topStart = startRadius,
                            bottomStart = startRadius,
                            topEnd = endRadius,
                            bottomEnd = endRadius,
                        ),
                        color = container,
                        contentColor = content,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(icon, null, Modifier.size(20.dp))
                            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
private fun SettingsScaffold(content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 144.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingsRow(section: SettingsSection, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(section.icon, null, Modifier.size(24.dp)) }
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(section.title, style = MaterialTheme.typography.titleMedium)
                Text(section.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccentChoice(
    palette: AccentPalette,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
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
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(colors.soft)
                    .then(if (selected) Modifier.border(2.dp, colors.strong, CircleShape) else Modifier),
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
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f).padding(start = 9.dp),
                maxLines = 1,
            )
            if (selected) Icon(Icons.Rounded.Check, "選択中", Modifier.size(18.dp), tint = colors.strong)
        }
    }
}

@Composable
private fun ZoneChoiceCard(
    zone: ZoneChoice,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val corner by animateDpAsState(if (selected) 24.dp else 14.dp, tween(220), label = "zoneCorner")
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(corner),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(if (selected) Icons.Rounded.Check else Icons.Rounded.Public, null, Modifier.size(20.dp))
            Text(
                zone.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun ThresholdStepper(label: String, value: Int, onValue: (Int) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    if (editing) {
        val minutes = input.toIntOrNull()?.takeIf { it in 0..999 }
        AlertDialog(onDismissRequest = { editing = false },
            title = { Text("${label}の時間") },
            text = {
                OutlinedTextField(value = input, onValueChange = { text ->
                    if (text.length <= 3 && text.all { it in '0'..'9' }) input = text
                }, label = { Text("分（0〜999・1分単位）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = minutes == null)
            },
            confirmButton = { TextButton(enabled = minutes != null, onClick = {
                minutes?.let(onValue); editing = false
            }) { Text("決定") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("キャンセル") } })
    }
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { onValue((value - 5).coerceAtLeast(0)) }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Rounded.Remove, "5分減らす")
            }
            Box(Modifier.size(width = 64.dp, height = 42.dp).clickable { input = value.toString(); editing = true }, contentAlignment = Alignment.Center) {
                Text("${value}分", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onValue((value + 5).coerceAtMost(999)) }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Rounded.Add, "5分増やす")
            }
        }
    }
}
