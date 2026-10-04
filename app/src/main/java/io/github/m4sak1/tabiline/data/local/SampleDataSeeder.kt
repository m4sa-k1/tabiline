package io.github.m4sak1.tabiline.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Adds the bundled demonstration journey once, without recreating data the user deletes later. */
class SampleDataSeeder(
    context: Context,
    private val database: TabilineDatabase,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun seedIfNeeded() {
        if (preferences.getBoolean(SEED_COMPLETE_KEY, false)) return

        database.withTransaction {
            val dao = database.dao()
            if (dao.tripCount() == 0) {
                val zone = ZoneId.systemDefault()
                val firstDay = LocalDate.now(zone)
                val now = System.currentTimeMillis()
                val tripId = dao.upsertTrip(
                    TripEntity(
                        name = "瀬戸内 3日間",
                        startEpochDay = firstDay.toEpochDay(),
                        endEpochDay = firstDay.plusDays(2).toEpochDay(),
                        note = "",
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                )

                sampleLegs(tripId, firstDay, zone).forEach { dao.upsertLeg(it) }
            }
        }

        preferences.edit { putBoolean(SEED_COMPLETE_KEY, true) }
    }

    private fun sampleLegs(tripId: Long, date: LocalDate, zone: ZoneId) = listOf(
        leg(
            tripId = tripId,
            date = date,
            departure = LocalTime.of(9, 12),
            arrival = LocalTime.of(9, 58),
            departurePlace = "高松",
            arrivalPlace = "琴平",
            mode = "TRAIN",
            trainType = "LIMITED_EXPRESS",
            platform = "2番のりば",
            memo = "6号車 12A",
            sortOrder = 0,
            zone = zone,
        ),
        leg(
            tripId = tripId,
            date = date,
            departure = LocalTime.of(10, 30),
            arrival = LocalTime.of(11, 20),
            departurePlace = "琴平",
            arrivalPlace = "高松",
            mode = "TRAIN",
            trainType = "LOCAL",
            platform = "1番のりば",
            memo = "",
            sortOrder = 1,
            zone = zone,
        ),
        leg(
            tripId = tripId,
            date = date,
            departure = LocalTime.of(11, 28),
            arrival = LocalTime.of(12, 20),
            departurePlace = "高松港",
            arrivalPlace = "土庄港",
            mode = "FERRY",
            trainType = null,
            platform = "乗り場2",
            memo = "",
            sortOrder = 2,
            zone = zone,
        ),
    )

    private fun leg(
        tripId: Long,
        date: LocalDate,
        departure: LocalTime,
        arrival: LocalTime,
        departurePlace: String,
        arrivalPlace: String,
        mode: String,
        trainType: String?,
        platform: String,
        memo: String,
        sortOrder: Int,
        zone: ZoneId,
    ) = TransportLegEntity(
        tripId = tripId,
        departureMillis = date.atTime(departure).atZone(zone).toInstant().toEpochMilli(),
        arrivalMillis = date.atTime(arrival).atZone(zone).toInstant().toEpochMilli(),
        departureZoneId = zone.id,
        arrivalZoneId = zone.id,
        departurePlace = departurePlace,
        arrivalPlace = arrivalPlace,
        mode = mode,
        trainType = trainType,
        departurePlatform = platform,
        arrivalPlatform = "",
        memo = memo,
        sortOrder = sortOrder,
    )

    private companion object {
        const val PREFERENCES_NAME = "tabiline_sample_data"
        const val SEED_COMPLETE_KEY = "sample_v1_complete"
    }
}
