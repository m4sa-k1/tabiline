package io.github.m4sak1.tabiline.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TripEntity::class, TransportLegEntity::class], version = 1, exportSchema = true)
abstract class TabilineDatabase : RoomDatabase() {
    abstract fun dao(): TabilineDao
}
