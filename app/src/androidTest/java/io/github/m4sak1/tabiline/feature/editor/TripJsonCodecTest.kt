package io.github.m4sak1.tabiline.feature.editor

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

internal val tripFixture = """{"format":"tabiline.trip","version":1,"trip":{"name":"京都旅行","startDate":"2026-11-14","endDate":"2026-11-15","note":"ゆったり"},"legs":[{"mode":"FLIGHT","departure":"2026-11-14T23:00:00+09:00","arrival":"2026-11-15T01:00:00+09:00","departureZoneId":"Asia/Tokyo","arrivalZoneId":"Asia/Tokyo","departurePlace":"羽田","arrivalPlace":"関西","departureTerminal":"第2","arrivalTerminal":"第1","departurePlatform":"10","arrivalPlatform":"20","boardingGroup":"2","flightNumber":"XX123","memo":"確認が必要","precedingGapType":"TRANSFER"},{"mode":"FREE_TIME","departure":"2026-11-15T02:00:00+09:00","arrival":"2026-11-15T03:00:00+09:00","departureZoneId":"Asia/Tokyo","arrivalZoneId":"Asia/Tokyo","departurePlace":"休憩","memo":"食事"}]}"""

class TripJsonCodecTest {
    @Test fun busAndSpecialRapidFieldsDecodeWithoutLosingPlatforms() {
        val root = JSONObject(tripFixture)
        val row = root.getJSONArray("legs").getJSONObject(0)
        row.put("mode", "BUS").put("busLine", "東京・大阪線").put("busType", "HIGHWAY_NIGHT")
        val bus = TripJsonCodec.decode(root.toString()).legs.first()
        assertEquals("東京・大阪線", bus.busLine)
        assertEquals(io.github.m4sak1.tabiline.core.model.BusType.HIGHWAY_NIGHT, bus.busType)
        assertEquals("10", bus.departurePlatform); assertEquals("20", bus.arrivalPlatform)
        row.put("mode", "TRAIN").put("trainType", "SPECIAL_RAPID").put("busType", JSONObject.NULL)
        assertEquals(io.github.m4sak1.tabiline.core.model.TrainType.SPECIAL_RAPID,
            TripJsonCodec.decode(root.toString()).legs.first().trainType)
        row.put("busType", "invalid"); rejects(root.toString())
    }
    @Test fun minifiedFencedAndBomJsonPreserveAllFieldsAndOvernight() {
        val plan = TripJsonCodec.decode("\uFEFF```json\n$tripFixture\n```")
        assertEquals("京都旅行", plan.trip.name)
        assertEquals(2, plan.legs.size)
        val leg = plan.legs.first()
        assertEquals("第2", leg.departureTerminal); assertEquals("第1", leg.arrivalTerminal)
        assertEquals("10", leg.departurePlatform); assertEquals("20", leg.arrivalPlatform)
        assertEquals("2", leg.boardingGroup); assertEquals("XX123", leg.flightNumber)
        assertEquals("確認が必要", leg.memo)
        assertEquals("休憩", plan.legs.last().departurePlace)
        assertEquals(0L, plan.trip.id); assertTrue(plan.legs.all { it.id == 0L && it.tripId == 0L })
    }
    @Test fun rejectsUnknownIdsDuplicateKeysAndLegacyFormat() {
        rejects(tripFixture.replace("\"name\":", "\"id\":12,\"name\":"))
        rejects(tripFixture.replace("\"mode\":\"FLIGHT\"", "\"mode\":\"FLIGHT\",\"tripId\":12"))
        rejects(tripFixture.replace("\"version\":1", "\"version\":1,\"version\":1"))
        rejects("{\"mode\":\"TRAIN\"}")
        rejects(tripFixture + "{}")
    }
    @Test fun rejectsBadDatesRangeTypesAndBrokenEntries() {
        rejects(tripFixture.replace("\"endDate\":\"2026-11-15\"", "\"endDate\":\"2026-11-14\""))
        rejects(tripFixture.replace("\"name\":\"京都旅行\"", "\"name\":null"))
        rejects(tripFixture.replace("2026-11-15T01:00:00+09:00", "2026-11-14T22:00:00+09:00"))
        rejects(tripFixture.replace("\"version\":1", "\"version\":2"))
        rejects(tripFixture.replace("2026-11-14T23:00:00+09:00", "2026-11-14T23:00:00"))
    }
    @Test fun rejectsLimitsAndSortsByDeparture() {
        val root = JSONObject(tripFixture)
        val legs = root.getJSONArray("legs")
        root.put("legs", org.json.JSONArray().put(legs.get(1)).put(legs.get(0)))
        assertEquals("XX123", TripJsonCodec.decode(root.toString()).legs.first().flightNumber)
        root.put("legs", org.json.JSONArray()); rejects(root.toString())
        val many = org.json.JSONArray(); repeat(201) { many.put(legs.get(0)) }
        root.put("legs", many); rejects(root.toString())
        rejects(" ".repeat(TripJsonCodec.MAX_BYTES + 1))
    }
    private fun rejects(text: String) { try { TripJsonCodec.decode(text); fail("Should reject") } catch (_: IllegalArgumentException) {} }
}
