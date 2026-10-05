package io.github.m4sak1.tabiline.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.ui.components.FooterLayout

/** Lives on the Trips screen, below the application's popup/scrim layer. */
@Composable
internal fun TripImportActions(enabled: Boolean, onPlanImported: (TripWithLegs) -> Unit, modifier: Modifier = Modifier) {
    var showAi by remember { mutableStateOf(false) }
    var showJson by remember { mutableStateOf(false) }
    var pasteMode by remember { mutableStateOf(false) }
    if (showAi) AiPromptPopup { showAi = false }
    if (showJson) TripJsonImportPopup({ showJson = false }, pasteMode, onPlanImported)
    BoxWithConstraints(modifier.fillMaxSize().navigationBarsPadding()) {
        Box(Modifier.align(Alignment.BottomEnd).padding(
            end = FooterLayout.menuEndPadding(maxWidth),
            bottom = FooterLayout.bottomPadding + FooterLayout.addSize + FooterLayout.menuGap)) {
            EditorImportMenu(enabled = enabled && !showAi && !showJson,
                onAi = { showAi = true }, onJson = { pasteMode = false; showJson = true },
                onPaste = { pasteMode = true; showJson = true })
        }
    }
}
