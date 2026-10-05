package io.github.m4sak1.tabiline.feature.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import io.github.m4sak1.tabiline.MainActivity
import org.junit.Rule
import org.junit.Test

class BackupPopupIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsOpensAndClosesBackupWithoutChangingData() {
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("設定").fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(6000)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("設定").performClick()
        compose.onNodeWithText("バックアップ").performScrollTo().performClick()
        compose.onNodeWithText("現在のデータを保存").assertIsDisplayed()
        compose.onNodeWithText("ファイルから復元").assertIsDisplayed()
        compose.onNodeWithText("ファイルは暗号化されません。メモや予約情報を含むため、安全な場所に保管してください。通知の許可とアプリアイコンの色は復元対象外です。").assertExists()
        compose.onNodeWithContentDescription("閉じる").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("現在のデータを保存").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithContentDescription("今日").assertIsDisplayed()
    }
}
