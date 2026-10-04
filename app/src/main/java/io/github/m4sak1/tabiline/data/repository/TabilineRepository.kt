package io.github.m4sak1.tabiline.data.repository

import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.Trip
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import kotlinx.coroutines.flow.Flow

interface TabilineRepository {
    fun observeTrips(): Flow<List<TripWithLegs>>
    fun observeTrip(id: Long): Flow<TripWithLegs?>
    suspend fun getLeg(id: Long): TransportLeg?
    suspend fun saveTrip(trip: Trip): Long
    suspend fun deleteTrip(id: Long)
    suspend fun saveLeg(leg: TransportLeg): Long
    suspend fun deleteLeg(id: Long)
    suspend fun reorderLegs(tripId: Long, orderedIds: List<Long>)
}
