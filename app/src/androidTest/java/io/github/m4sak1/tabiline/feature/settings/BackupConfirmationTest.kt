package io.github.m4sak1.tabiline.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BackupConfirmationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun acknowledgementRequiredAndBusyCannotRestore() {
        var acknowledged by mutableStateOf(false)
        var busy by mutableStateOf(false)
        var restores = 0
        compose.setContent { MaterialTheme {
            BackupConfirmation(acknowledged, busy, { acknowledged = it }) { restores++ }
        } }
        compose.onNodeWithText("現在のデータはすべて上書きされます").assertIsDisplayed()
        compose.onNodeWithText("事前にバックアップしていない既存データは、復元後に取り戻せません。旅行・移動・空き時間はすべて置き換わります。").assertIsDisplayed()
        compose.onNodeWithText("現在のデータを上書きして復元").assertIsNotEnabled()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("現在のデータを上書きして復元").assertIsEnabled().performClick()
        assertEquals(1, restores)
        compose.runOnIdle { busy = true }
        compose.onNodeWithText("現在のデータを上書きして復元").assertIsNotEnabled()
        compose.onNode(isToggleable()).assertIsNotEnabled()
    }
}
