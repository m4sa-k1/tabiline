package io.github.m4sak1.tabiline.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class, TransportLegEntity::class], version = 6, exportSchema = true)
abstract class TabilineDatabase : RoomDatabase() {
    abstract fun dao(): TabilineDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transport_legs ADD COLUMN busLine TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE transport_legs ADD COLUMN busType TEXT")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                listOf("departureTerminal", "arrivalTerminal", "boardingGroup", "flightNumber").forEach { column ->
                    database.execSQL("ALTER TABLE transport_legs ADD COLUMN $column TEXT NOT NULL DEFAULT ''")
                }
            }
        }

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE transport_legs ADD COLUMN trainLine TEXT NOT NULL DEFAULT ''",
                )
                database.execSQL(
                    "UPDATE transport_legs SET arrivalMillis = departureMillis + 3600000 " +
                        "WHERE mode = 'FREE_TIME' AND arrivalMillis <= departureMillis",
                )
            }
        }
    }
}
