package io.github.m4sak1.tabiline.data.backup

import io.github.m4sak1.tabiline.core.model.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class BackupDocument(
    val createdAt: Instant,
    val appVersion: String,
    val trips: List<TripWithLegs>,
    val settings: UserSettings,
) {
    val legCount get() = trips.sumOf { it.legs.size }
}

/** Explicit, versioned interchange format; never serializes SQLite internals or Android permissions. */
object BackupCodec {
    const val MAX_BYTES = 10 * 1024 * 1024
    private const val FORMAT = "io.github.m4sak1.tabiline.backup"

    fun encode(document: BackupDocument): String {
        validate(document)
        val settings = document.settings
        val root = JSONObject().put("format", FORMAT).put("version", 1)
            .put("createdAt", document.createdAt.toString()).put("appVersion", document.appVersion)
            .put("settings", JSONObject().put("theme", settings.theme.name)
                .put("accentPalette", settings.accentPalette.name).put("defaultZoneId", settings.defaultZoneId)
                .put("notificationsEnabled", settings.notificationsEnabled)
                .put("footerBlurEnabled", settings.footerBlurEnabled)
                .put("notificationMinutes", JSONObject().apply {
                    TransportMode.entries.forEach { put(it.name, settings.notificationMinutes.getValue(it)) }
                }).put("trainMinutes", settings.trainMinutes).put("busMinutes", settings.busMinutes)
                .put("flightMinutes", settings.flightMinutes).put("ferryMinutes", settings.ferryMinutes)
                .put("otherMinutes", settings.otherMinutes))
            .put("trips", JSONArray().apply {
                document.trips.forEach { row ->
                    val trip = row.trip
                    put(JSONObject().put("id", trip.id).put("name", trip.name)
                        .put("startDate", trip.startDate.toString()).put("endDate", trip.endDate.toString())
                        .put("note", trip.note).put("createdAt", trip.createdAt.toString())
                        .put("updatedAt", trip.updatedAt.toString()).put("isAutomatic", trip.isAutomatic)
                        .put("legs", JSONArray().apply { row.legs.forEach { put(encodeLeg(it)) } }))
                }
            })
        return root.toString(2).also {
            require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "バックアップが大きすぎます（最大10MB）。" }
        }
    }

    private fun encodeLeg(leg: TransportLeg) = JSONObject()
        .put("id", leg.id).put("tripId", leg.tripId).put("departure", leg.departure.toString())
        .put("arrival", leg.arrival.toString()).put("departureZoneId", leg.departureZoneId)
        .put("arrivalZoneId", leg.arrivalZoneId).put("departurePlace", leg.departurePlace)
        .put("arrivalPlace", leg.arrivalPlace).put("mode", leg.mode.name)
        .put("trainType", leg.trainType?.name ?: JSONObject.NULL).put("trainLine", leg.trainLine)
        .put("departureTerminal", leg.departureTerminal).put("arrivalTerminal", leg.arrivalTerminal)
        .put("boardingGroup", leg.boardingGroup).put("flightNumber", leg.flightNumber)
        .put("departurePlatform", leg.departurePlatform).put("arrivalPlatform", leg.arrivalPlatform)
        .put("memo", leg.memo).put("sortOrder", leg.sortOrder).put("precedingGapType", leg.precedingGapType.name)

    fun decode(text: String): BackupDocument {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "ファイルが大きすぎます（最大10MB）。" }
        try {
            val parser = JSONTokener(text)
            val root = parser.nextValue() as? JSONObject ?: throw IllegalArgumentException("バックアップの形式が不正です。")
            require(parser.nextClean() == '\u0000') { "ファイルの末尾に不正なデータがあります。" }
            require(root.string("format") == FORMAT) { "Tabilineのバックアップファイルではありません。" }
            require(root.number("version") == 1L) { "このバックアップ形式には対応していません。アプリを更新してください。" }
            val config = root.getJSONObject("settings")
            val minutes = config.getJSONObject("notificationMinutes")
            val settings = UserSettings(
                footerBlurEnabled = if (config.has("footerBlurEnabled")) config.boolean("footerBlurEnabled") else true,
                theme = ThemePreference.valueOf(config.string("theme")),
                accentPalette = AccentPalette.valueOf(config.string("accentPalette")),
                defaultZoneId = config.string("defaultZoneId"),
                notificationsEnabled = config.boolean("notificationsEnabled"),
                notificationMinutes = TransportMode.entries.associateWith { minutes.minute(it.name) },
                trainMinutes = config.minute("trainMinutes"), busMinutes = config.minute("busMinutes"),
                flightMinutes = config.minute("flightMinutes"), ferryMinutes = config.minute("ferryMinutes"),
                otherMinutes = config.minute("otherMinutes"),
            )
            val rows = root.getJSONArray("trips")
            require(rows.length() <= 10000) { "旅行の件数が多すぎます。" }
            var legCount = 0
            val trips = (0 until rows.length()).map { index ->
                val row = rows.getJSONObject(index)
                val trip = Trip(row.number("id"), row.string("name"), LocalDate.parse(row.string("startDate")),
                    LocalDate.parse(row.string("endDate")), row.string("note"),
                    Instant.parse(row.string("createdAt")), Instant.parse(row.string("updatedAt")), row.boolean("isAutomatic"))
                val legs = row.getJSONArray("legs")
                legCount += legs.length()
                require(legCount <= 100000) { "予定の件数が多すぎます。" }
                TripWithLegs(trip, (0 until legs.length()).map { decodeLeg(legs.getJSONObject(it)) })
            }
            return BackupDocument(Instant.parse(root.string("createdAt")), root.string("appVersion"), trips, settings)
                .also(::validate)
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Exception) {
            throw IllegalArgumentException("バックアップが壊れているか、必要な情報が不足しています。", error)
        }
    }

    private fun decodeLeg(row: JSONObject) = TransportLeg(
        id = row.number("id"), tripId = row.number("tripId"),
        departure = Instant.parse(row.string("departure")), arrival = Instant.parse(row.string("arrival")),
        departureZoneId = row.string("departureZoneId"), arrivalZoneId = row.string("arrivalZoneId"),
        departurePlace = row.string("departurePlace"), arrivalPlace = row.string("arrivalPlace"),
        mode = TransportMode.valueOf(row.string("mode")),
        trainType = if (row.get("trainType") == JSONObject.NULL) null else TrainType.valueOf(row.string("trainType")),
        trainLine = row.string("trainLine"), departureTerminal = row.string("departureTerminal"),
        arrivalTerminal = row.string("arrivalTerminal"), boardingGroup = row.string("boardingGroup"),
        flightNumber = row.string("flightNumber"), departurePlatform = row.string("departurePlatform"),
        arrivalPlatform = row.string("arrivalPlatform"), memo = row.string("memo"),
        sortOrder = row.number("sortOrder").also { require(it in 0..Int.MAX_VALUE.toLong()) }.toInt(),
        precedingGapType = GapType.valueOf(row.string("precedingGapType")),
    )

    fun validate(document: BackupDocument) {
        document.createdAt.toEpochMilli()
        checkText(document.appVersion)
        require(document.trips.size <= 10000 && document.legCount <= 100000) { "データの件数が多すぎます。" }
        val tripIds = mutableSetOf<Long>()
        val legIds = mutableSetOf<Long>()
        document.trips.forEach { row ->
            val trip = row.trip
            trip.createdAt.toEpochMilli(); trip.updatedAt.toEpochMilli()
            require(trip.id < Long.MAX_VALUE) { "旅行の識別情報が不正です。" }
            require(trip.id > 0 && tripIds.add(trip.id)) { "旅行の識別情報が重複または不正です。" }
            require(trip.endDate >= trip.startDate) { "旅行の日付が不正です。" }
            require(trip.name.isNotBlank()) { "旅行名がありません。" }
            checkText(trip.name); checkText(trip.note)
            val orders = mutableSetOf<Int>()
            row.legs.forEach { leg ->
                leg.departure.toEpochMilli(); leg.arrival.toEpochMilli()
                require(leg.id < Long.MAX_VALUE) { "予定の識別情報が不正です。" }
                require(leg.id > 0 && legIds.add(leg.id) && leg.tripId == trip.id) { "予定の識別情報や旅行との関連が不正です。" }
                require(leg.arrival >= leg.departure) { "終了時刻が開始時刻より前になっています。" }
                require(leg.sortOrder >= 0 && orders.add(leg.sortOrder)) { "予定の並び順が不正です。" }
                ZoneId.of(leg.departureZoneId); ZoneId.of(leg.arrivalZoneId)
                listOf(leg.departurePlace, leg.arrivalPlace, leg.trainLine, leg.departureTerminal,
                    leg.arrivalTerminal, leg.boardingGroup, leg.flightNumber, leg.departurePlatform,
                    leg.arrivalPlatform, leg.memo).forEach(::checkText)
            }
        }
        val settings = document.settings
        ZoneId.of(settings.defaultZoneId)
        require(TransportMode.entries.all { settings.notificationMinutes[it] in 0..999 } &&
            listOf(settings.trainMinutes, settings.busMinutes, settings.flightMinutes,
                settings.ferryMinutes, settings.otherMinutes).all { it in 0..999 }) { "通知・警告の時間が不正です。" }
    }

    private fun checkText(text: String) { require(text.length <= 20000) { "文字数が上限を超えています。" } }
    private fun JSONObject.string(key: String): String = (get(key) as? String
        ?: throw IllegalArgumentException("項目 $key の形式が不正です。")).also(::checkText)
    private fun JSONObject.boolean(key: String) = get(key) as? Boolean
        ?: throw IllegalArgumentException("項目 $key の形式が不正です。")
    private fun JSONObject.number(key: String): Long = when (val value = get(key)) {
        is Int -> value.toLong()
        is Long -> value
        else -> throw IllegalArgumentException("項目 $key の数値が不正です。")
    }
    private fun JSONObject.minute(key: String) = number(key).also {
        require(it in 0..999) { "通知・警告の時間が不正です。" }
    }.toInt()
}
