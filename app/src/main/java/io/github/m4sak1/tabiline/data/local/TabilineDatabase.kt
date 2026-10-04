package io.github.m4sak1.tabiline.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class, TransportLegEntity::class], version = 3, exportSchema = true)
abstract class TabilineDatabase : RoomDatabase() {
    abstract fun dao(): TabilineDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE trips ADD COLUMN isAutomatic INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE transport_legs ADD COLUMN precedingGapType TEXT NOT NULL DEFAULT 'WAIT'",
                )
            }
        }
    }
}
