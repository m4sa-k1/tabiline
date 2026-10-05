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
        compose.onNodeWithText("行き先が決まっている").performScrollTo().performClick()
        compose.onNodeWithText("県・地域が決まっている").performClick()
        compose.onNodeWithText("旅行先でしたいこと（要入力）").assertExists()
        compose.onNodeWithText("出発地（要入力）").performScrollTo().performTextInput("東京駅")
        compose.onNodeWithText("プロンプトをコピー").performScrollTo().performClick()
        compose.onNodeWithText("コピーしました。【要入力：…】を入力してからAIへ送ってください。").assertExists()
        compose.runOnIdle {
            val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(AiPromptTemplates.create(AiPlanStyle.REGION, AiPromptOptions(mapOf(AiPromptField.FROM to "東京駅"))), clipboard.primaryClip?.getItemAt(0)?.text?.toString())
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

    @Test fun detailedConditionsPaceAndTransportAppearInCopiedPrompt() {
        compose.setContent { TabilineTheme(ThemePreference.LIGHT, AccentPalette.GREEN) { AiPromptPopup {} } }
        compose.onNodeWithText("詳細な条件をカスタマイズ").performScrollTo().performClick()
        compose.onNodeWithText("乗り換えの余裕（任意）").performScrollTo().performTextInput("20分")
        compose.onNodeWithText("ゆったり").performScrollTo().performClick()
        compose.onNodeWithText("電車").performScrollTo().performClick()
        compose.onNodeWithText("徒歩").performScrollTo().performClick()
        compose.onNodeWithText("プロンプトをコピー").performScrollTo().performClick()
        compose.runOnIdle {
            val clipboard = InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val prompt = clipboard.primaryClip!!.getItemAt(0).text.toString()
            assertTrue(prompt.contains("乗り換えの余裕：20分"))
            assertTrue(prompt.contains("旅のペース：ゆったり"))
            assertTrue(prompt.contains("徒歩・電車"))
            assertTrue(prompt.contains("tabiline.trip"))
        }
    }

    @Test fun closingUncopiedConditionsAsksBeforeDiscarding() {
        var dismissed = false
        compose.setContent { TabilineTheme(ThemePreference.DARK, AccentPalette.GREEN) { AiPromptPopup { dismissed = true } } }
        compose.onNodeWithText("出発地（要入力）").performScrollTo().performTextInput("東京駅")
        compose.onNodeWithContentDescription("AIプロンプトを閉じる").performScrollTo().performClick()
        compose.onNodeWithText("入力内容を破棄しますか？").assertIsDisplayed()
        compose.onNodeWithText("入力を続ける").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("入力を続ける").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("東京駅").assertExists()
        assertFalse(dismissed)
    }
}
