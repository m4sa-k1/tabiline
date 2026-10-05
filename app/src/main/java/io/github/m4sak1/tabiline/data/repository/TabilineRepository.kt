package io.github.m4sak1.tabiline.data.repository

import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.GapType
import io.github.m4sak1.tabiline.core.model.Trip
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import kotlinx.coroutines.flow.Flow

interface TabilineRepository {
    suspend fun snapshotTrips(): List<TripWithLegs>
    suspend fun replaceTrips(trips: List<TripWithLegs>, beforeCommit: suspend () -> Unit = {})
    fun observeTrips(): Flow<List<TripWithLegs>>
    fun observeTrip(id: Long): Flow<TripWithLegs?>
    suspend fun getLeg(id: Long): TransportLeg?
    suspend fun saveTrip(trip: Trip): Long
    suspend fun createTripWithLegs(plan: TripWithLegs): Long
    suspend fun deleteTrip(id: Long)
    suspend fun saveLeg(leg: TransportLeg): Long
    suspend fun saveStandaloneLeg(leg: TransportLeg): Long
    suspend fun deleteLeg(id: Long)
    suspend fun reorderLegs(tripId: Long, orderedIds: List<Long>)
    suspend fun updateGapType(legId: Long, gapType: GapType)
}
