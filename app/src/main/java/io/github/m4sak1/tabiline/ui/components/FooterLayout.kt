package io.github.m4sak1.tabiline.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.m4sak1.tabiline.core.model.FooterBlurMode

internal object FooterLayout {
    val contentWidth = 330.dp // 4 × 52 + 3 × 2 + 12 + 104
    val selectorHeight = 52.dp
    val addSize = 104.dp
    val bottomPadding = 12.dp
    val menuSize = 56.dp
    val menuGap = 12.dp
    fun menuEndPadding(viewportWidth: Dp) = ((viewportWidth - contentWidth) / 2 + (addSize - menuSize) / 2).coerceAtLeast(0.dp)
}

internal fun FooterBlurMode.backdropHeight(footerHeight: Dp): Dp =
    (footerHeight - if (this == FooterBlurMode.FOOTER_ONLY) FooterLayout.addSize - FooterLayout.selectorHeight else 0.dp)
        .coerceAtLeast(0.dp) + 48.dp
