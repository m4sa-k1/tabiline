package io.github.m4sak1.tabiline.ui.theme

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

/** Native pickers must follow the app preference, not just the device theme. */
@Composable
fun themedDialogContext(): Context = ContextThemeWrapper(LocalContext.current,
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) android.R.style.Theme_Material_Dialog_Alert
    else android.R.style.Theme_Material_Light_Dialog_Alert)
