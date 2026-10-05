package io.github.m4sak1.tabiline.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.m4sak1.tabiline.core.model.BusType
import io.github.m4sak1.tabiline.data.repository.OfflineTabilineRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BusMigrationTest {
    @Test fun migratesVersionFiveWithoutLosingLegsAndPersistsNewBusFields() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "bus-migration-${System.nanoTime()}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE trips (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, startEpochDay INTEGER NOT NULL, endEpochDay INTEGER NOT NULL, note TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL, isAutomatic INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE transport_legs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tripId INTEGER NOT NULL, departureMillis INTEGER NOT NULL, arrivalMillis INTEGER NOT NULL, departureZoneId TEXT NOT NULL, arrivalZoneId TEXT NOT NULL, departurePlace TEXT NOT NULL, arrivalPlace TEXT NOT NULL, mode TEXT NOT NULL, trainType TEXT, trainLine TEXT NOT NULL, departureTerminal TEXT NOT NULL, arrivalTerminal TEXT NOT NULL, boardingGroup TEXT NOT NULL, flightNumber TEXT NOT NULL, departurePlatform TEXT NOT NULL, arrivalPlatform TEXT NOT NULL, memo TEXT NOT NULL, sortOrder INTEGER NOT NULL, precedingGapType TEXT NOT NULL, FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX index_transport_legs_tripId ON transport_legs(tripId)")
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table VALUES(42, '3e13347ed67251f0638aac20e0435e09')")
            db.execSQL("INSERT INTO trips VALUES(1, '保存済み旅行', 20000, 20000, '', 0, 0, 0)")
            db.execSQL("INSERT INTO transport_legs VALUES(1, 1, 0, 3600000, 'Asia/Tokyo', 'Asia/Tokyo', '東京', '大阪', 'BUS', NULL, '', '', '', '', '', '1', '2', '既存メモ', 0, 'WAIT')")
            db.version = 5
        }
        val db = Room.databaseBuilder(context, TabilineDatabase::class.java, name)
            .addMigrations(TabilineDatabase.MIGRATION_5_6).build()
        try {
            val repository = OfflineTabilineRepository(db, db.dao())
            val old = repository.snapshotTrips().single().legs.single()
            assertEquals("既存メモ", old.memo); assertEquals("1", old.departurePlatform)
            assertEquals("", old.busLine); assertNull(old.busType)
            repository.saveLeg(old.copy(busLine = "東京・大阪線", busType = BusType.HIGHWAY_NIGHT))
            val saved = repository.snapshotTrips().single().legs.single()
            assertEquals("東京・大阪線", saved.busLine); assertEquals(BusType.HIGHWAY_NIGHT, saved.busType)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
