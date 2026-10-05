package io.github.m4sak1.tabiline.feature.editor

import org.junit.Assert.*
import org.junit.Test

class AiPromptTemplatesTest {
    @Test fun everyStyleContainsPlaceholdersAndFullImportContract() {
        val prompts = AiPlanStyle.entries.map(AiPromptTemplates::create)
        assertEquals(4, prompts.toSet().size)
        prompts.forEach { prompt ->
            assertTrue(prompt.startsWith("あなたは"))
            listOf("【要入力：", "【任意：", "tabiline.transport-leg", "256KB", "1件ずつ",
                "departure", "arrival", "departureZoneId", "arrivalZoneId", "departurePlace", "arrivalPlace",
                "trainType", "trainLine", "departureTerminal", "arrivalTerminal", "departurePlatform",
                "arrivalPlatform", "boardingGroup", "flightNumber", "memo", "precedingGapType", "tripName",
                "tripId", "FREE_TIME", "TRANSFER", "SHINKANSEN", "公式情報", "未記入", "推測")
                .forEach { assertTrue("Missing $it", prompt.contains(it)) }
            assertFalse(prompt.contains("\uFFFD"))
        }
    }
}
