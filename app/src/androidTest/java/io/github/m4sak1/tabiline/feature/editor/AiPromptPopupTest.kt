package io.github.m4sak1.tabiline.feature.editor

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AiPromptPopupTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectedPromptCopiesCompleteContractAndInputInstructions() {
        var dismissed = false
        compose.setContent { TabilineTheme(ThemePreference.DARK, AccentPalette.GREEN) {
            AiPromptPopup { dismissed = true }
        } }
        AiPlanStyle.entries.forEach { compose.onNodeWithText(it.title).assertExists() }
        compose.onNodeWithText("県・地域が決まっている").performScrollTo().performClick()
        compose.onNodeWithText("プロンプトをコピー").performScrollTo().performClick()
        compose.onNodeWithText("コピーしました。【要入力：…】を入力してからAIへ送ってください。").assertExists()
        compose.runOnIdle {
            val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(AiPromptTemplates.create(AiPlanStyle.REGION), clipboard.primaryClip?.getItemAt(0)?.text?.toString())
            assertFalse(dismissed)
        }
    }

    @Test fun menuSeparatesAiFileAndPasteActions() {
        var action = ""
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.PURPLE) {
            EditorImportMenu(true, { action = "ai" }, { action = "file" }, { action = "paste" })
        } }
        compose.onNodeWithContentDescription("追加メニューを開く").performClick()
        listOf("AIに考えてもらう", "JSONを読み込む", "JSONをコピペで読み込む").forEach {
            compose.onNodeWithText(it).assertIsDisplayed().assertIsEnabled()
        }
        compose.onNodeWithText("JSONを読み込む").performClick()
        compose.runOnIdle { assertEquals("file", action) }
        compose.onNodeWithContentDescription("追加メニューを開く").performClick()
        compose.onNodeWithText("JSONをコピペで読み込む").performClick()
        compose.runOnIdle { assertEquals("paste", action) }
        compose.onNodeWithContentDescription("追加メニューを開く").performClick()
        compose.onNodeWithText("AIに考えてもらう").performClick()
        compose.runOnIdle { assertEquals("ai", action) }
    }
}
