package io.github.m4sak1.tabiline.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import io.github.m4sak1.tabiline.MainActivity
import org.junit.Rule
import org.junit.Test

class FooterVisibilityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun footerIsAvailableWithoutAnOpenEditor() {
        compose.waitUntil(15000) {
            compose.onAllNodesWithContentDescription("設定").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        listOf("今日", "タイムライン", "旅行", "設定").forEach {
            compose.onNodeWithContentDescription(it).assertIsDisplayed()
        }
        compose.onNodeWithContentDescription("移動を追加").assertIsDisplayed()
        compose.onNodeWithContentDescription("設定").performClick()
        compose.onNodeWithContentDescription("今日").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("移動を追加").assertIsDisplayed()
    }
}
