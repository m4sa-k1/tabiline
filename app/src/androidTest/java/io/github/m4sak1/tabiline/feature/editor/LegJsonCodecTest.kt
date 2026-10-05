package io.github.m4sak1.tabiline.feature.editor

import io.github.m4sak1.tabiline.core.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class LegJsonCodecTest {
    private val trips = listOf(Trip(id = 12, name = "東京旅行", startDate = LocalDate.of(2026, 11, 14), endDate = LocalDate.of(2026, 11, 15)))
    private fun json(mode: TransportMode = TransportMode.FLIGHT) = JSONObject()
        .put("format", "tabiline.transport-leg").put("version", 1).put("mode", mode.name)
        .put("departure", "2026-11-14T19:41:00+09:00").put("arrival", "2026-11-15T07:48:00+09:00")
        .put("departureZoneId", "Asia/Tokyo").put("arrivalZoneId", "Asia/Tokyo")
        .put("departurePlace", "出発").put("arrivalPlace", "到着")
        .put("tripName", "東京旅行").put("departureTerminal", "T1").put("arrivalTerminal", "T2")
        .put("departurePlatform", "10").put("arrivalPlatform", "58")
        .put("boardingGroup", "2").put("flightNumber", "NH20").put("memo", "座席メモ")
        .put("trainLine", "山手線").put("trainType", "LOCAL").put("precedingGapType", "TRANSFER")

    @Test fun allFieldsAndOvernightTimesDecode() {
        val leg = LegJsonCodec.decode(json().toString(), trips, null)
        assertEquals(12L, leg.tripId); assertEquals(0L, leg.id); assertEquals(0, leg.sortOrder)
        assertEquals(Instant.parse("2026-11-14T10:41:00Z"), leg.departure)
        assertEquals(Instant.parse("2026-11-14T22:48:00Z"), leg.arrival)
        assertEquals("Asia/Tokyo", leg.departureZoneId); assertEquals("Asia/Tokyo", leg.arrivalZoneId)
        assertEquals("出発", leg.departurePlace); assertEquals("到着", leg.arrivalPlace)
        assertEquals(TransportMode.FLIGHT, leg.mode); assertEquals(TrainType.LOCAL, leg.trainType)
        assertEquals("山手線", leg.trainLine); assertEquals("T1", leg.departureTerminal); assertEquals("T2", leg.arrivalTerminal)
        assertEquals("10", leg.departurePlatform); assertEquals("58", leg.arrivalPlatform)
        assertEquals("2", leg.boardingGroup); assertEquals("NH20", leg.flightNumber)
        assertEquals("座席メモ", leg.memo); assertEquals(GapType.TRANSFER, leg.precedingGapType)
    }

    @Test fun everyTransportModeAndFreeTimeAreSupported() {
        TransportMode.entries.forEach { mode -> assertEquals(mode, LegJsonCodec.decode(json(mode).toString(), trips, 12).mode) }
        val free = json(TransportMode.FREE_TIME).apply { remove("departurePlace"); remove("arrivalPlace") }
        assertEquals("", LegJsonCodec.decode(free.toString(), trips, null).departurePlace)
    }

    @Test fun tripSelectionAndMarkdownWork() {
        val row = json().apply { remove("tripName") }
        assertEquals(12L, LegJsonCodec.decode(row.toString(), trips, 12).tripId)
        row.put("tripId", JSONObject.NULL)
        assertEquals(0L, LegJsonCodec.decode("```json\n$row\n```", trips, 12).tripId)
        row.put("tripId", 12)
        assertEquals(12L, LegJsonCodec.decode(row.toString(), trips, null).tripId)
    }

    @Test fun invalidInputsAndDatabaseIdsRejected() {
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.put("arrival", "2020-01-01T00:00:00Z") }, { it.remove("departure") },
            { it.put("departureZoneId", "Wrong/Zone") }, { it.put("departurePlace", "") },
            { it.put("mode", "TRAINISH") }, { it.put("memo", 5) }, { it.put("version", 2) },
            { it.put("id", 20) }, { it.put("sortOrder", 0) }, { it.put("tripName", "ない旅行") },
            { it.put("tripId", 12) }, { it.put("trainLine", "a".repeat(20001)) },
        )
        mutations.forEach { mutate -> assertThrows(IllegalArgumentException::class.java) {
            LegJsonCodec.decode(json().apply(mutate).toString(), trips, null)
        } }
        assertThrows(IllegalArgumentException::class.java) { LegJsonCodec.decode(json().toString() + "junk", trips, null) }
        assertThrows(IllegalArgumentException::class.java) { LegJsonCodec.decode(" ".repeat(LegJsonCodec.MAX_BYTES + 1), trips, null) }
        assertThrows(IllegalArgumentException::class.java) { LegJsonCodec.decode("[]", trips, null) }
    }

    @Test fun oneLineJsonEscapesUnicodeAndFencesAreExact() {
        val row = json().put("memo", "引用\"符・改行\n・パス C:\\旅\\予約・東京駅・😀・```json")
        val compact = row.toString()
        assertFalse(compact.contains('\n'))
        val expected = LegJsonCodec.decode(row.toString(2), trips, null)
        assertEquals(expected, LegJsonCodec.decode(compact, trips, null))
        assertEquals(expected, LegJsonCodec.decode("```json $compact ```", trips, null))
        assertEquals(expected, LegJsonCodec.decode("\uFEFF$compact", trips, null))
        assertEquals(expected, LegJsonCodec.decode(compact.replace("東京駅", "\\u6771\\u4eac\\u99c5"), trips, null))
    }

    @Test fun ambiguousOrNonJsonSyntaxIsRejected() {
        val compact = json().toString()
        listOf(compact.dropLast(1) + ",\"mode\":\"TRAIN\"}", compact.dropLast(1) + ",}",
            "{mode:'TRAIN'}", "/* comment */$compact", compact + compact).forEach {
            assertThrows(IllegalArgumentException::class.java) { LegJsonCodec.decode(it, trips, null) }
        }
    }

    @Test fun promptExampleMatchesActualImportSchema() {
        val contract = AiPromptTemplates.schemaInstructions
        val example = contract.substring(contract.indexOf('{'), contract.indexOf('}') + 1)
        assertEquals(TransportMode.TRAIN, LegJsonCodec.decode(example, emptyList(), null).mode)
    }
}
