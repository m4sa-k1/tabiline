package io.github.m4sak1.tabiline.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TabilineDao {
    @Query("DELETE FROM transport_legs")
    suspend fun deleteAllLegs()

    @Query("DELETE FROM trips")
    suspend fun deleteAllTrips()

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun tripCount(): Int

    @Transaction
    @Query("SELECT * FROM trips ORDER BY startEpochDay ASC, createdAtMillis ASC")
    fun observeTrips(): Flow<List<TripWithLegsEntity>>

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun observeTrip(tripId: Long): Flow<TripWithLegsEntity?>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTrip(tripId: Long): TripEntity?

    @Query("SELECT * FROM trips WHERE isAutomatic = 1 AND startEpochDay = :departureEpochDay LIMIT 1")
    suspend fun getAutomaticTrip(departureEpochDay: Long): TripEntity?

    @Query("SELECT * FROM transport_legs WHERE tripId = :tripId ORDER BY sortOrder")
    suspend fun getLegsForTrip(tripId: Long): List<TransportLegEntity>

    @Query("SELECT * FROM transport_legs WHERE id = :legId")
    suspend fun getLeg(legId: Long): TransportLegEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrip(trip: TripEntity): Long

    @Update suspend fun updateTrip(trip: TripEntity)
    @Delete suspend fun deleteTrip(trip: TripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeg(leg: TransportLegEntity): Long

    @Delete suspend fun deleteLeg(leg: TransportLegEntity)

    @Query("UPDATE transport_legs SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateLegOrder(id: Long, sortOrder: Int)

    @Query("UPDATE transport_legs SET precedingGapType = :gapType WHERE id = :id")
    suspend fun updatePrecedingGapType(id: Long, gapType: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM transport_legs WHERE tripId = :tripId")
    suspend fun nextSortOrder(tripId: Long): Int

    @Query("SELECT COUNT(*) FROM transport_legs WHERE tripId = :tripId AND departureMillis <= :departureMillis")
    suspend fun chronologicalInsertionOrder(tripId: Long, departureMillis: Long): Int

    @Query("UPDATE transport_legs SET sortOrder = sortOrder + 1 WHERE tripId = :tripId AND sortOrder >= :fromOrder")
    suspend fun makeSpaceForOrder(tripId: Long, fromOrder: Int)
}
