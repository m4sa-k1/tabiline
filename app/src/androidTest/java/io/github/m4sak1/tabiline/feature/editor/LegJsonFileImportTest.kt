package io.github.m4sak1.tabiline.feature.editor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class LegJsonFileImportTest {
    @get:Rule val compose = createComposeRule()

    private fun registry(uri: Uri?) = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                dispatchResult(requestCode, if (uri == null) Activity.RESULT_CANCELED else Activity.RESULT_OK, Intent().setData(uri))
            }
        }
    }

    @Test fun fileMenuAutomaticallyLoadsAFileWithoutPasteControls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("json-import-test", ".json", context.cacheDir)
        file.writeText("""{"mode":"FREE_TIME","departure":"2026-11-14T09:00:00+09:00","arrival":"2026-11-14T10:00:00+09:00","departureZoneId":"Asia/Tokyo","arrivalZoneId":"Asia/Tokyo","departurePlace":"昼食","memo":"予約確認"}""")
        var applied: TransportLeg? = null
        try {
            val owner = registry(Uri.fromFile(file))
            compose.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) {
                    LegJsonImportPopup(emptyList(), null, {}, pasteMode = false) { applied = it }
                }
            } }
            compose.waitUntil(5000) { compose.onAllNodesWithText("ファイルを読み込みました。入力欄へ反映して確認できます。").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("JSONを貼り付け").assertDoesNotExist()
            compose.onNodeWithText("クリップボードから貼り付け").assertDoesNotExist()
            assertNull(applied)
            compose.onNodeWithText("入力欄へ反映").performScrollTo().performClick()
            compose.waitUntil(5000) { applied != null }
            assertEquals("昼食", applied?.departurePlace)
            assertEquals("予約確認", applied?.memo)
        } finally { file.delete() }
    }

    @Test fun cancellingFileSelectionClosesWithoutApplying() {
        var dismissed = false
        val owner = registry(null)
        compose.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
            TabilineTheme(ThemePreference.DARK, AccentPalette.PURPLE) {
                LegJsonImportPopup(emptyList(), null, { dismissed = true }, pasteMode = false) { fail("Should not apply") }
            }
        } }
        compose.waitUntil(5000) { dismissed }
    }
}
