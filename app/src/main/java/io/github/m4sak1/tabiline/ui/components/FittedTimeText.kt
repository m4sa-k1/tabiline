package io.github.m4sak1.tabiline.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Keep the timeline rail stationary; fit even relative-day labels and enlarged system fonts. */
@Composable
internal fun FittedTimeText(text: String, maxSize: TextUnit, weight: FontWeight, modifier: Modifier = Modifier) {
    BasicText(text, modifier = modifier, maxLines = 1, softWrap = false,
        style = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current, fontWeight = weight),
        autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = maxSize, stepSize = 0.5.sp))
}
