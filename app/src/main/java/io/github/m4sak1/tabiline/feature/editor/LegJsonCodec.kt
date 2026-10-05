package io.github.m4sak1.tabiline.feature.editor

import io.github.m4sak1.tabiline.core.model.*
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Instant
import java.time.ZoneId
import android.util.JsonReader
import android.util.JsonToken
import java.io.StringReader

/** AI interchange contract, distinct from backups. New-entry IDs and ordering remain app-managed. */
object LegJsonCodec {
    const val MAX_BYTES = 256 * 1024
    private val keys = setOf("format", "version", "tripId", "tripName", "departure", "arrival",
        "departureZoneId", "arrivalZoneId", "departurePlace", "arrivalPlace", "mode", "trainType", "trainLine",
        "departureTerminal", "arrivalTerminal", "boardingGroup", "flightNumber", "departurePlatform",
        "arrivalPlatform", "memo", "precedingGapType")

    fun decode(source: String, trips: List<Trip>, currentTripId: Long?): TransportLeg {
        require(source.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "JSONが大きすぎます（最大256KB）。" }
        var text = source.trim().removePrefix("\uFEFF").trim()
        if (text.startsWith("```")) {
            val fenced = Regex("```(?:json)?\\s*(\\{[\\s\\S]*\\})\\s*```", RegexOption.IGNORE_CASE).matchEntire(text)
            require(fenced != null) { "JSONの囲みを確認してください。" }
            text = fenced.groupValues[1]
        }
        try {
            JsonReader(StringReader(text)).use { reader ->
                reader.isLenient = false
                require(reader.peek() == JsonToken.BEGIN_OBJECT) { "1件の移動を表すJSONオブジェクトが必要です。" }
                validateJson(reader, 0)
                require(reader.peek() == JsonToken.END_DOCUMENT) { "JSONの後ろに余分な内容があります。" }
            }
            val parser = JSONTokener(text)
            val row = parser.nextValue() as? JSONObject ?: throw IllegalArgumentException("1件の移動を表すJSONオブジェクトが必要です。")
            require(parser.nextClean() == '\u0000') { "JSONの後ろに余分な内容があります。" }
            row.keys().forEach { require(it in keys) { "対応していない項目があります：$it" } }
            if (row.has("format")) require(row.text("format") == "tabiline.transport-leg") { "移動追加用のJSONではありません。" }
            if (row.has("version")) require(row.get("version") == 1) { "このJSONのバージョンには対応していません。" }
            val selectable = trips.filterNot { it.isAutomatic }
            require(!(row.has("tripId") && row.has("tripName"))) { "旅行の指定はtripIdかtripNameの片方にしてください。" }
            val tripId = when {
                row.has("tripId") -> when (val value = row.get("tripId")) {
                    JSONObject.NULL -> null
                    is Int -> value.toLong()
                    is Long -> value
                    else -> throw IllegalArgumentException("tripIdの形式が不正です。")
                }
                row.has("tripName") -> if (row.get("tripName") == JSONObject.NULL) null else {
                    val matches = selectable.filter { it.name == row.text("tripName") }
                    require(matches.size == 1) { "旅行名を一意に特定できません。旅行の指定を確認してください。" }
                    matches.single().id
                }
                else -> currentTripId
            }
            require(tripId == null || selectable.any { it.id == tripId }) { "指定された旅行が見つかりません。" }
            val mode = TransportMode.valueOf(row.text("mode"))
            val departureZone = ZoneId.of(row.text("departureZoneId"))
            val arrivalZone = ZoneId.of(row.text("arrivalZoneId"))
            val departure = Instant.parse(row.text("departure"))
            val arrival = Instant.parse(row.text("arrival"))
            departure.toEpochMilli(); arrival.toEpochMilli()
            require(arrival > departure) { "終了・到着時刻は開始・出発時刻より後にしてください。" }
            require(mode != TransportMode.FREE_TIME || departureZone == arrivalZone) { "空き時間の開始と終了は同じタイムゾーンにしてください。" }
            val from = row.optional("departurePlace")
            val to = row.optional("arrivalPlace")
            require(mode == TransportMode.FREE_TIME || from.isNotBlank() && to.isNotBlank()) { "出発地と到着地が必要です。" }
            return TransportLeg(tripId = tripId ?: 0, departure = departure, arrival = arrival,
                departureZoneId = departureZone.id, arrivalZoneId = arrivalZone.id,
                departurePlace = from, arrivalPlace = to, mode = mode,
                trainType = if (!row.has("trainType") || row.isNull("trainType")) {
                    if (mode == TransportMode.TRAIN) TrainType.LOCAL else null
                } else TrainType.valueOf(row.text("trainType")),
                trainLine = row.optional("trainLine"), departureTerminal = row.optional("departureTerminal"),
                arrivalTerminal = row.optional("arrivalTerminal"), boardingGroup = row.optional("boardingGroup"),
                flightNumber = row.optional("flightNumber"), departurePlatform = row.optional("departurePlatform"),
                arrivalPlatform = row.optional("arrivalPlatform"), memo = row.optional("memo"),
                precedingGapType = if (row.has("precedingGapType")) GapType.valueOf(row.text("precedingGapType")) else GapType.WAIT)
        } catch (error: IllegalArgumentException) {
            if (error.message?.any { it.code > 127 } == true) throw error
            throw IllegalArgumentException("日時・タイムゾーン・交通手段などの値が不正です。", error)
        } catch (error: Exception) {
            throw IllegalArgumentException("JSONが壊れているか、必要な項目が不足しています。", error)
        }
    }

    private fun JSONObject.text(key: String) = (get(key) as? String
        ?: throw IllegalArgumentException("${key}は文字列で指定してください。")).also {
            require(it.length <= 20000) { "${key}が長すぎます。" }
        }
    private fun JSONObject.optional(key: String) = if (has(key)) text(key) else ""

    private fun validateJson(reader: JsonReader, depth: Int) {
        require(depth <= 8) { "JSONの入れ子が深すぎます。" }
        when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                val names = mutableSetOf<String>()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    require(names.add(name)) { "項目名が重複しています：$name" }
                    validateJson(reader, depth + 1)
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) validateJson(reader, depth + 1)
                reader.endArray()
            }
            JsonToken.STRING, JsonToken.NUMBER -> reader.nextString()
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NULL -> reader.nextNull()
            else -> throw IllegalArgumentException("JSONの形式が不正です。")
        }
    }
}
