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
import java.time.ZoneId
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
        return database.withTransaction {
            val oldTripId = leg.id.takeIf { it != 0L }?.let { dao.getLeg(it)?.tripId }
            val savedId = saveLegInTransaction(leg)
            oldTripId?.takeIf { it != leg.tripId }?.let { compactOrders(it); refreshAutomaticTrip(it) }
            refreshAutomaticTrip(leg.tripId)
            savedId
        }
    }

    override suspend fun saveStandaloneLeg(leg: TransportLeg): Long = database.withTransaction {
        val departureDate = leg.departure.atZone(ZoneId.of(leg.departureZoneId)).toLocalDate()
        val oldTripId = leg.id.takeIf { it != 0L }?.let { dao.getLeg(it)?.tripId }
        val automaticTripId = dao.getAutomaticTrip(departureDate.toEpochDay())?.id ?: dao.upsertTrip(
            TripEntity(
                name = departureDate.shortName(),
                startEpochDay = departureDate.toEpochDay(),
                endEpochDay = leg.arrival.atZone(ZoneId.of(leg.arrivalZoneId)).toLocalDate().toEpochDay(),
                note = "",
                createdAtMillis = Instant.now().toEpochMilli(),
                updatedAtMillis = Instant.now().toEpochMilli(),
                isAutomatic = true,
            ),
        )
        saveLegInTransaction(leg.copy(tripId = automaticTripId))
        oldTripId?.takeIf { it != automaticTripId }?.let { compactOrders(it); refreshAutomaticTrip(it) }
        refreshAutomaticTrip(automaticTripId)
        automaticTripId
    }

    override suspend fun deleteLeg(id: Long) {
        database.withTransaction {
            dao.getLeg(id)?.let {
                dao.deleteLeg(it)
                compactOrders(it.tripId)
                refreshAutomaticTrip(it.tripId)
            }
        }
    }

    override suspend fun reorderLegs(tripId: Long, orderedIds: List<Long>) {
        database.withTransaction {
            orderedIds.forEachIndexed { index, id -> dao.updateLegOrder(id, index) }
        }
    }

    private suspend fun saveLegInTransaction(leg: TransportLeg): Long {
        val previous = leg.id.takeIf { it != 0L }?.let { dao.getLeg(it) }
        if (previous != null && previous.tripId == leg.tripId) {
            return dao.upsertLeg(leg.toEntity().copy(sortOrder = previous.sortOrder))
        }
        val order = dao.chronologicalInsertionOrder(leg.tripId, leg.departure.toEpochMilli())
        dao.makeSpaceForOrder(leg.tripId, order)
        return dao.upsertLeg(leg.toEntity().copy(sortOrder = order))
    }

    private suspend fun compactOrders(tripId: Long) {
        dao.getLegsForTrip(tripId).forEachIndexed { index, leg ->
            if (leg.sortOrder != index) dao.updateLegOrder(leg.id, index)
        }
    }

    private suspend fun refreshAutomaticTrip(tripId: Long) {
        val trip = dao.getTrip(tripId)?.takeIf { it.isAutomatic } ?: return
        val legs = dao.getLegsForTrip(tripId)
        if (legs.isEmpty()) {
            dao.deleteTrip(trip)
            return
        }
        val firstDate = legs.minOf {
            Instant.ofEpochMilli(it.departureMillis).atZone(ZoneId.of(it.departureZoneId)).toLocalDate()
        }
        val lastDate = legs.maxOf {
            Instant.ofEpochMilli(it.arrivalMillis).atZone(ZoneId.of(it.arrivalZoneId)).toLocalDate()
        }
        dao.updateTrip(
            trip.copy(
                name = firstDate.shortName(),
                startEpochDay = firstDate.toEpochDay(),
                endEpochDay = lastDate.toEpochDay(),
                updatedAtMillis = Instant.now().toEpochMilli(),
            ),
        )
    }
}

private fun LocalDate.shortName() = "$monthValue/$dayOfMonth"

private fun TripEntity.toModel() = Trip(
    id, name, LocalDate.ofEpochDay(startEpochDay), LocalDate.ofEpochDay(endEpochDay), note,
    Instant.ofEpochMilli(createdAtMillis), Instant.ofEpochMilli(updatedAtMillis), isAutomatic,
)

private fun Trip.toEntity() = TripEntity(
    id, name, startDate.toEpochDay(), endDate.toEpochDay(), note,
    createdAt.toEpochMilli(), updatedAt.toEpochMilli(), isAutomatic,
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
