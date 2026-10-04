package io.github.m4sak1.tabiline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.R
import io.github.m4sak1.tabiline.core.model.AccentPalette
import io.github.m4sak1.tabiline.core.model.ThemePreference

data class AccentColors(val strong: Color, val medium: Color, val soft: Color)

fun accentColors(palette: AccentPalette): AccentColors = when (palette) {
    AccentPalette.PURPLE -> AccentColors(Color(0xFF675496), Color(0xFF8874B8), Color(0xFFC8B3FD))
    AccentPalette.ORCHID -> AccentColors(Color(0xFF86468C), Color(0xFFAA69B0), Color(0xFFFBAFFE))
    AccentPalette.BLUE -> AccentColors(Color(0xFF315DA8), Color(0xFF5D82C6), Color(0xFFAFCBFF))
    AccentPalette.GREEN -> AccentColors(Color(0xFF367A55), Color(0xFF5D9B76), Color(0xFFAEEAC5))
    AccentPalette.CORAL -> AccentColors(Color(0xFFA84462), Color(0xFFC76B82), Color(0xFFFFC0CD))
    AccentPalette.AMBER -> AccentColors(Color(0xFF925700), Color(0xFFB87817), Color(0xFFFFD18A))
    AccentPalette.TEAL -> AccentColors(Color(0xFF006A70), Color(0xFF328E93), Color(0xFF8EE8EB))
    AccentPalette.MONO -> AccentColors(Color(0xFF514B5E), Color(0xFF767080), Color(0xFFD5CFDF))
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF4A4459), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7DFF8), onPrimaryContainer = Color.Black,
    secondary = Color(0xFF48464D), secondaryContainer = Color(0xFFE4E1EA),
    onSecondaryContainer = Color(0xFF312F36), tertiaryContainer = Color(0xFFF0DEE2),
    onTertiaryContainer = Color(0xFF3A2D30), surface = Color(0xFFFAF8FE),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF4F3F8),
    surfaceContainer = Color(0xFFEFEDF3), surfaceContainerHigh = Color(0xFFE9E7ED),
    surfaceContainerHighest = Color(0xFFE3E2E7), onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF303036), outline = Color(0xFF605D68),
    outlineVariant = Color(0xFFA09CA8), inverseSurface = Color(0xFF313034),
    inverseOnSurface = Color(0xFFF2F0F5), inversePrimary = Color(0xFFCAC3DC),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD8D1EA), onPrimary = Color(0xFF332D41),
    primaryContainer = Color(0xFF4A4459), onPrimaryContainer = Color(0xFFF3EEFE),
    secondary = Color(0xFFD6D3DC), secondaryContainer = Color(0xFF48464D),
    onSecondaryContainer = Color(0xFFF2EFF8), tertiaryContainer = Color(0xFF524346),
    onTertiaryContainer = Color(0xFFFFECF0), surface = Color(0xFF141317),
    surfaceContainerLowest = Color(0xFF0F0E12), surfaceContainerLow = Color(0xFF1C1B1F),
    surfaceContainer = Color(0xFF201F23), surfaceContainerHigh = Color(0xFF2B292D),
    surfaceContainerHighest = Color(0xFF353438), onSurface = Color(0xFFE3E2E7),
    onSurfaceVariant = Color(0xFFD6D2DF), outline = Color(0xFFADA9B6),
    outlineVariant = Color(0xFF797581), inverseSurface = Color(0xFFE3E2E7),
    inverseOnSurface = Color(0xFF313034), inversePrimary = Color(0xFF615B71),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
)

private val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex_regular, FontWeight.Normal),
    Font(R.font.roboto_flex_medium, FontWeight.Medium),
    Font(R.font.roboto_flex_semibold, FontWeight.SemiBold),
    Font(R.font.roboto_flex_bold, FontWeight.Bold),
    Font(R.font.roboto_flex_extrabold, FontWeight.ExtraBold),
)

private val TabilineTypography = Typography().run {
    copy(
        displayLarge = displayLarge.withFont(FontWeight.Bold), displayMedium = displayMedium.withFont(FontWeight.Bold),
        displaySmall = displaySmall.withFont(FontWeight.Bold), headlineLarge = headlineLarge.withFont(FontWeight.Bold),
        headlineMedium = headlineMedium.withFont(FontWeight.Bold), headlineSmall = headlineSmall.withFont(FontWeight.Bold),
        titleLarge = titleLarge.withFont(FontWeight.Bold), titleMedium = titleMedium.withFont(FontWeight.Bold),
        titleSmall = titleSmall.withFont(FontWeight.Bold), bodyLarge = bodyLarge.withFont(),
        bodyMedium = bodyMedium.withFont(), bodySmall = bodySmall.withFont(),
        labelLarge = labelLarge.withFont(FontWeight.Bold), labelMedium = labelMedium.withFont(FontWeight.Medium),
        labelSmall = labelSmall.withFont(FontWeight.Medium),
    )
}

private fun TextStyle.withFont(weight: FontWeight = fontWeight ?: FontWeight.Normal) =
    copy(
        fontFamily = RobotoFlex,
        fontWeight = weight,
        fontStyle = FontStyle.Normal,
        fontSynthesis = FontSynthesis.None,
    )

@Composable
fun TabilineTheme(preference: ThemePreference, accentPalette: AccentPalette, content: @Composable () -> Unit) {
    val dark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val accent = accentColors(accentPalette)
    val colors = if (dark) {
        DarkColors.copy(
            primary = accent.medium,
            onPrimary = Color.White,
            primaryContainer = accent.strong,
            onPrimaryContainer = Color.White,
            inversePrimary = accent.strong,
        )
    } else {
        LightColors.copy(
            primary = accent.medium,
            onPrimary = Color.White,
            primaryContainer = accent.soft,
            onPrimaryContainer = Color.Black,
            inversePrimary = accent.soft,
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = TabilineTypography,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
    ) {
        // Plain Text composables also use the bundled family instead of an OEM font fallback.
        ProvideTextStyle(value = TabilineTypography.bodyLarge, content = content)
    }
}
