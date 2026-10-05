package io.github.m4sak1.tabiline.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.ui.components.*
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class FooterBackdropTest {
    @get:Rule val compose = createComposeRule()
    @Test fun blursBackgroundOnlyAndLeavesControlsCrisp() {
        var enabled by mutableStateOf(false)
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
            val state = remember { HazeState() }
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize().hazeSource(state)) {
                    val stripe = 8.dp.toPx()
                    repeat((size.width / stripe).toInt() + 1) { index ->
                        drawRect(if (index % 2 == 0) Color.Black else Color.White,
                            Offset(index * stripe, 0f), Size(stripe, size.height))
                    }
                }
                if (enabled) FooterBackdrop(state, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(180.dp))
                AppBottomBar(AppDestination.TODAY, Modifier.align(Alignment.BottomCenter), onAdd = {}, onSelect = {})
            }
        } }
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bounds = compose.onRoot().fetchSemanticsNode().boundsInWindow
        val density = instrumentation.targetContext.resources.displayMetrics.density
        val x = (bounds.left + 20 * density).toInt()
        val y = (bounds.bottom - 35 * density).toInt()
        val icon = compose.onNodeWithContentDescription("追加").fetchSemanticsNode().boundsInWindow.center
        val before = instrumentation.uiAutomation.takeScreenshot()
        compose.runOnIdle { enabled = true }
        compose.waitForIdle()
        Thread.sleep(600) // Let the hardware backdrop render its captured frame.
        val after = instrumentation.uiAutomation.takeScreenshot()
        try {
            assertTrue("Footer backdrop must alter the striped background", abs(AndroidColor.red(before.getPixel(x, y)) - AndroidColor.red(after.getPixel(x, y))) > 30)
            assertEquals("Foreground icon must not blur", before.getPixel(icon.x.toInt(), icon.y.toInt()), after.getPixel(icon.x.toInt(), icon.y.toInt()))
            // Content above the gradient remains completely untouched.
            val top = (bounds.top + 40 * density).toInt()
            assertEquals(before.getPixel(x, top), after.getPixel(x, top))
        } finally { before.recycle(); after.recycle() }
    }
}
