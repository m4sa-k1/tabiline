package io.github.m4sak1.tabiline.feature.editor

import android.util.JsonReader
import android.util.JsonToken
import io.github.m4sak1.tabiline.core.model.Trip
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import org.json.JSONObject
import java.io.StringReader
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Creates a new trip only. Never accepts saved IDs or overwrites existing trips. */
object TripJsonCodec {
    const val MAX_BYTES = 1024 * 1024
    const val MAX_LEGS = 200

    fun decode(source: String): TripWithLegs {
        require(source.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "JSONが大きすぎます（最大1MB）。" }
        var text = source.trim().removePrefix("\uFEFF").trim()
        if (text.startsWith("```")) {
            text = Regex("```(?:json)?\\s*(\\{[\\s\\S]*\\})\\s*```", RegexOption.IGNORE_CASE)
                .matchEntire(text)?.groupValues?.get(1) ?: throw IllegalArgumentException("JSONの囲みを確認してください。")
        }
        try {
            JsonReader(StringReader(text)).use { reader ->
                reader.isLenient = false
                require(reader.peek() == JsonToken.BEGIN_OBJECT) { "旅行全体を表すJSONオブジェクトが必要です。" }
                LegJsonCodec.validateJson(reader, 0)
                require(reader.peek() == JsonToken.END_DOCUMENT) { "JSONの後ろに余分な内容があります。" }
            }
            val root = JSONObject(text)
            root.onlyKeys(setOf("format", "version", "trip", "legs"))
            require(root.get("format") == "tabiline.trip" && root.get("version") == 1) { "旅行追加用のJSON（tabiline.trip / version 1）を選んでください。" }
            val row = root.getJSONObject("trip")
            row.onlyKeys(setOf("name", "startDate", "endDate", "note"))
            val name = row.string("name").trim()
            require(name.isNotEmpty()) { "旅行名が必要です。" }
            val start = LocalDate.parse(row.string("startDate"))
            val end = LocalDate.parse(row.string("endDate"))
            require(end >= start && ChronoUnit.DAYS.between(start, end) < 366) { "旅行期間は開始日以降、最大366日で指定してください。" }
            val trip = Trip(name = name, startDate = start, endDate = end, note = if (row.has("note")) row.string("note") else "")
            val array = root.getJSONArray("legs")
            require(array.length() in 1..MAX_LEGS) { "予定は1〜200件で指定してください。" }
            val legs = (0 until array.length()).map { index ->
                try {
                    val leg = array.getJSONObject(index)
                    require(!leg.has("tripId") && !leg.has("tripName")) { "予定のtripId・tripNameは不要です。新しい旅行にまとめて紐づきます。" }
                    LegJsonCodec.decode(leg.toString(), emptyList(), null)
                } catch (failure: Exception) {
                    throw IllegalArgumentException("予定${index + 1}：${failure.message ?: "形式が不正です。"}", failure)
                }
            }.sortedBy { it.departure }.mapIndexed { index, leg -> leg.copy(sortOrder = index) }
            validateRange(trip, legs.map { it.departureLocal.toLocalDate() to it.arrivalLocal.toLocalDate() })
            return TripWithLegs(trip, legs)
        } catch (failure: IllegalArgumentException) {
            if (failure.message?.any { it.code > 127 } == true) throw failure
            throw IllegalArgumentException("旅行の日付・予定の形式を確認してください。", failure)
        } catch (failure: Exception) {
            throw IllegalArgumentException("旅行全体のJSONが壊れているか、必須項目が不足しています。", failure)
        }
    }

    internal fun validateRange(trip: Trip, dates: List<Pair<LocalDate, LocalDate>>) {
        require(dates.all { (from, to) -> from in trip.startDate..trip.endDate && to in trip.startDate..trip.endDate }) {
            "旅行期間外の予定があります。日またぎの到着日も旅行期間に含めてください。"
        }
    }

    private fun JSONObject.onlyKeys(allowed: Set<String>) = keys().forEach {
        require(it in allowed) { "対応していない項目があります：$it" }
    }
    private fun JSONObject.string(key: String): String = (get(key) as? String
        ?: throw IllegalArgumentException("${key}は文字列で指定してください。")) .also {
        require(it.length <= 20000) { "${key}が長すぎます。" }
    }
}
