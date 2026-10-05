package io.github.m4sak1.tabiline.feature.editor

import org.junit.Assert.*
import org.junit.Test

class AiPromptTemplatesTest {
    @Test fun everyStyleContainsPlaceholdersAndFullImportContract() {
        val prompts = AiPlanStyle.entries.map { AiPromptTemplates.create(it) }
        assertEquals(9, prompts.toSet().size)
        prompts.forEach { prompt ->
            assertTrue(prompt.startsWith("あなたは"))
            listOf("【要入力：", "【任意：", "tabiline.trip", "1MB", "legs", "startDate", "endDate",
                "departure", "arrival", "departureZoneId", "arrivalZoneId", "departurePlace", "arrivalPlace",
                "trainType", "trainLine", "departureTerminal", "arrivalTerminal", "departurePlatform",
                "arrivalPlatform", "boardingGroup", "flightNumber", "memo", "precedingGapType", "tripName",
                "tripId", "FREE_TIME", "TRANSFER", "SHINKANSEN", "公式情報", "未記入", "推測")
                .forEach { assertTrue("Missing $it", prompt.contains(it)) }
            assertFalse(prompt.contains("\uFFFD"))
        }
    }
    @Test fun allStylesAskForActivitiesAndSeishunUsesCurrentRules() {
        AiPlanStyle.entries.forEach { style ->
            assertTrue(AiPromptField.MUST in AiPromptTemplates.requiredFields(style))
            assertTrue(AiPromptTemplates.create(style).contains("旅行先でしたいこと：【要入力："))
        }
        val prompt = AiPromptTemplates.create(AiPlanStyle.SEISHUN18)
        listOf("連続", "1枚につき1人", "公式情報", "利用期間外", "別料金", "5回分").forEach { assertTrue(prompt.contains(it)) }
    }
    @Test fun customizationIsIncludedWithoutReplacingUnfilledRequiredFields() {
        val prompt = AiPromptTemplates.create(AiPlanStyle.MULTI_STOP, AiPromptOptions(
            values = mapOf(AiPromptField.FROM to "東京", AiPromptField.DESTINATION to "京都・大阪", AiPromptField.BUFFER to "20分"),
            pace = "ゆったり", transport = setOf("電車", "徒歩")))
        assertTrue(prompt.contains("出発地：東京"))
        assertTrue(prompt.contains("目的地：京都・大阪"))
        assertTrue(prompt.contains("乗り換えの余裕：20分"))
        assertTrue(prompt.contains("旅のペース：ゆったり"))
        assertTrue(prompt.contains("徒歩・電車"))
        assertTrue(prompt.contains("【要入力：YYYY-MM-DD"))
    }
}
