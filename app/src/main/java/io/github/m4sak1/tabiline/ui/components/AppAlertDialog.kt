package io.github.m4sak1.tabiline.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/** Confirmation/numeric-entry dialogs use the same blur, scrim and close animation as other popups. */
typealias PopupCloseAction = (() -> Unit) -> Unit

@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (PopupCloseAction) -> Unit,
    dismissButton: (@Composable (PopupCloseAction) -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(true) }
    var actionAfterClose by remember { mutableStateOf<(() -> Unit)?>(null) }
    val latestDismiss by rememberUpdatedState(onDismissRequest)
    val close: PopupCloseAction = { action -> if (visible) { actionAfterClose = action; visible = false } }
    val behind = LocalView.current.rootView
    DisposableEffect(behind) { onDispose { if (Build.VERSION.SDK_INT >= 31) behind.setRenderEffect(null) } }
    Dialog(onDismissRequest = { visible = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) }
        CenterPopup(visible, onHidden = {
            actionAfterClose?.invoke()
            latestDismiss()
        }) { progress, motion ->
            SideEffect {
                if (Build.VERSION.SDK_INT >= 31) {
                    val radius = 20f * behind.resources.displayMetrics.density * progress
                    behind.setRenderEffect(if (radius > 0f) RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP) else null)
                }
            }
            Box(Modifier.fillMaxSize().clickable { visible = false })
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(24.dp), contentAlignment = Alignment.Center) {
                Surface(modifier = motion.widthIn(max = 440.dp).fillMaxWidth().heightIn(max = maxHeight),
                    shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        icon?.let { Box(Modifier.align(Alignment.CenterHorizontally)) { it() } }
                        title?.let { ProvideTextStyle(MaterialTheme.typography.headlineSmall) { it() } }
                        text?.let { ProvideTextStyle(MaterialTheme.typography.bodyMedium) { it() } }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                            dismissButton?.invoke(close)
                            confirmButton(close)
                        }
                    }
                }
            }
        }
    }
}
