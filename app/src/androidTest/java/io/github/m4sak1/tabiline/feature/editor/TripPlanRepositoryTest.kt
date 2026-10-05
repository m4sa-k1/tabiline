package io.github.m4sak1.tabiline.feature.editor

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.m4sak1.tabiline.data.local.TabilineDatabase
import io.github.m4sak1.tabiline.data.repository.OfflineTabilineRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TripPlanRepositoryTest {
    @Test fun createsNewTripAndBindsEveryLegWithoutReplacingExistingData() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, TabilineDatabase::class.java).build()
        try {
            val repository = OfflineTabilineRepository(db, db.dao())
            val plan = TripJsonCodec.decode(tripFixture)
            val first = repository.createTripWithLegs(plan)
            val second = repository.createTripWithLegs(plan)
            val trips = repository.snapshotTrips()
            assertEquals(2, trips.size); assertNotEquals(first, second)
            assertTrue(trips.all { it.legs.size == 2 && it.legs.all { leg -> leg.tripId == it.trip.id && leg.id > 0L } })
        } finally { db.close() }
    }

    @Test fun insertionFailureRollsBackTripAndPreviouslyInsertedLegs() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, TabilineDatabase::class.java).build()
        try {
            val repository = OfflineTabilineRepository(db, db.dao())
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_plan BEFORE INSERT ON transport_legs WHEN NEW.memo = '食事' BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
            try { repository.createTripWithLegs(TripJsonCodec.decode(tripFixture)); fail("Should fail on second entry") }
            catch (_: android.database.SQLException) {}
            assertTrue(repository.snapshotTrips().isEmpty())
        } finally { db.close() }
    }
}
