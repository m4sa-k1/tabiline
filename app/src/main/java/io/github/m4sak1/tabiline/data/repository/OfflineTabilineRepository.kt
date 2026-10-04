package io.github.m4sak1.tabiline.data.repository

import androidx.room.withTransaction
import io.github.m4sak1.tabiline.core.model.TrainType
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.TransportMode
import io.github.m4sak1.tabiline.core.model.Trip
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.data.local.TabilineDao
import io.github.m4sak1.tabiline.data.local.TabilineDatabase
import io.github.m4sak1.tabiline.data.local.TransportLegEntity
import io.github.m4sak1.tabiline.data.local.TripEntity
import io.github.m4sak1.tabiline.data.local.TripWithLegsEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineTabilineRepository(
    private val database: TabilineDatabase,
    private val dao: TabilineDao,
) : TabilineRepository {
    override fun observeTrips(): Flow<List<TripWithLegs>> =
        dao.observeTrips().map { rows -> rows.map(TripWithLegsEntity::toModel) }

    override fun observeTrip(id: Long): Flow<TripWithLegs?> =
        dao.observeTrip(id).map { it?.toModel() }

    override suspend fun getLeg(id: Long): TransportLeg? = dao.getLeg(id)?.toModel()

    override suspend fun saveTrip(trip: Trip): Long {
        val now = Instant.now()
        val entity = trip.copy(updatedAt = now).toEntity()
        return if (trip.id == 0L) dao.upsertTrip(entity.copy(createdAtMillis = now.toEpochMilli()))
        else { dao.updateTrip(entity); trip.id }
    }

    override suspend fun deleteTrip(id: Long) {
        dao.getTrip(id)?.let { dao.deleteTrip(it) }
    }

    override suspend fun saveLeg(leg: TransportLeg): Long {
        if (leg.id != 0L) return dao.upsertLeg(leg.toEntity())
        return database.withTransaction {
            val order = dao.chronologicalInsertionOrder(leg.tripId, leg.departure.toEpochMilli())
            dao.makeSpaceForOrder(leg.tripId, order)
            dao.upsertLeg(leg.toEntity().copy(sortOrder = order))
        }
    }

    override suspend fun deleteLeg(id: Long) {
        dao.getLeg(id)?.let { dao.deleteLeg(it) }
    }

    override suspend fun reorderLegs(tripId: Long, orderedIds: List<Long>) {
        database.withTransaction {
            orderedIds.forEachIndexed { index, id -> dao.updateLegOrder(id, index) }
        }
    }
}

private fun TripEntity.toModel() = Trip(
    id, name, LocalDate.ofEpochDay(startEpochDay), LocalDate.ofEpochDay(endEpochDay), note,
    Instant.ofEpochMilli(createdAtMillis), Instant.ofEpochMilli(updatedAtMillis),
)

private fun Trip.toEntity() = TripEntity(
    id, name, startDate.toEpochDay(), endDate.toEpochDay(), note,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(),
)

private fun TransportLegEntity.toModel() = TransportLeg(
    id, tripId, Instant.ofEpochMilli(departureMillis), Instant.ofEpochMilli(arrivalMillis),
    departureZoneId, arrivalZoneId, departurePlace, arrivalPlace,
    TransportMode.valueOf(mode), trainType?.let(TrainType::valueOf), departurePlatform,
    arrivalPlatform, memo, sortOrder,
)

private fun TransportLeg.toEntity() = TransportLegEntity(
    id, tripId, departure.toEpochMilli(), arrival.toEpochMilli(), departureZoneId, arrivalZoneId,
    departurePlace, arrivalPlace, mode.name, trainType?.name, departurePlatform, arrivalPlatform,
    memo, sortOrder,
)

private fun TripWithLegsEntity.toModel() = TripWithLegs(
    trip.toModel(), legs.sortedBy { it.sortOrder }.map(TransportLegEntity::toModel),
)
