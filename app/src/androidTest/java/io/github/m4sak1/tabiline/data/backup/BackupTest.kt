package io.github.m4sak1.tabiline.data.backup

import android.net.Uri
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.data.local.TabilineDatabase
import io.github.m4sak1.tabiline.data.repository.OfflineTabilineRepository
import io.github.m4sak1.tabiline.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate

class BackupTest {
    private val now = Instant.parse("2026-10-05T10:00:00Z")
    private fun sample(): BackupDocument {
        val trip = Trip(51, "東京旅行", LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-07"), "予約メモ", now, now)
        val legs = TransportMode.entries.mapIndexed { index, mode ->
            TransportLeg(100L + index, trip.id, now.plusSeconds(index * 86400L), now.plusSeconds(index * 86400L + 7200),
                "Asia/Tokyo", "Europe/London", "出発", "到着", mode, TrainType.LOCAL, "山手線",
                "T1", "T2", "Group 2", "JL123", "Gate 1", "Gate 2", "座席メモ", index, GapType.TRANSFER)
        }
        val standalone = trip.copy(id = 52, name = "10/5", isAutomatic = true)
        return BackupDocument(now, "1.0.1", listOf(TripWithLegs(trip, legs), TripWithLegs(standalone, emptyList())),
            UserSettings(true, TransportMode.entries.associateWith { 23 }, ThemePreference.DARK, AccentPalette.TEAL,
                "Europe/London", 17, 18, 19, 20, 21))
    }

    @Test fun allFieldsRoundTrip() { assertEquals(sample(), BackupCodec.decode(BackupCodec.encode(sample()))) }
    @Test fun busFieldsAndBlurModeRoundTripWithLegacyDefaults() {
        val sample = sample()
        val document = sample.copy(settings = sample.settings.copy(footerBlurMode = FooterBlurMode.FOOTER_ONLY),
            trips = sample.trips.map { it.copy(legs = it.legs.map { leg ->
                if (leg.mode == TransportMode.BUS) leg.copy(busLine = "京都市バス205系統", busType = BusType.LOCAL) else leg
            }) })
        assertEquals(document, BackupCodec.decode(BackupCodec.encode(document)))
        val json = JSONObject(BackupCodec.encode(document))
        json.getJSONObject("settings").remove("footerBlurMode")
        val rows = json.getJSONArray("trips").getJSONObject(0).getJSONArray("legs")
        repeat(rows.length()) { rows.getJSONObject(it).remove("busLine"); rows.getJSONObject(it).remove("busType") }
        val restored = BackupCodec.decode(json.toString())
        assertEquals(FooterBlurMode.WITH_ADD_BUTTON, restored.settings.footerBlurMode)
        assertTrue(restored.trips.flatMap { it.legs }.all { it.busLine.isEmpty() && it.busType == null })
    }
    @Test fun footerPreferenceRoundTripsAndOlderBackupsUseDefault() {
        val document = sample().copy(settings = sample().settings.copy(footerBlurEnabled = false))
        assertFalse(BackupCodec.decode(BackupCodec.encode(document)).settings.footerBlurEnabled)
        val legacy = JSONObject(BackupCodec.encode(document))
        legacy.getJSONObject("settings").remove("footerBlurEnabled")
        assertTrue(BackupCodec.decode(legacy.toString()).settings.footerBlurEnabled)
        legacy.getJSONObject("settings").put("footerBlurEnabled", "false")
        assertThrows(Exception::class.java) { BackupCodec.decode(legacy.toString()) }
    }

    @Test fun malformedFilesRejected() {
        val encoded = BackupCodec.encode(sample())
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.put("version", 99) }, { it.remove("settings") },
            { it.getJSONArray("trips").getJSONObject(0).put("id", 1.5) },
            { it.getJSONArray("trips").getJSONObject(1).put("id", 51) },
            { it.getJSONObject("settings").put("defaultZoneId", "Invalid/Zone") },
            { it.getJSONArray("trips").getJSONObject(0).getJSONArray("legs").getJSONObject(0).put("arrival", "2020-01-01T00:00:00Z") },
            { it.getJSONArray("trips").getJSONObject(0).getJSONArray("legs").getJSONObject(0).remove("trainType") },
            { it.getJSONObject("settings").put("trainMinutes", 1000) },
        )
        mutations.forEach { mutation ->
            val json = JSONObject(encoded).apply(mutation)
            assertThrows(Exception::class.java) { BackupCodec.decode(json.toString()) }
        }
        assertThrows(Exception::class.java) { BackupCodec.decode(encoded + "junk") }
        assertThrows(Exception::class.java) { BackupCodec.decode("not JSON") }
        assertThrows(Exception::class.java) { BackupCodec.decode(" ".repeat(BackupCodec.MAX_BYTES + 1)) }
    }

    @Test fun replacementIsAtomicAndSupportsEmptyBackup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, TabilineDatabase::class.java).build()
        try {
            val repository = OfflineTabilineRepository(database, database.dao())
            val old = sample().trips
            repository.replaceTrips(old)
            try { repository.replaceTrips(emptyList()) { error("simulated failure before commit") }; fail("Expected failure") }
            catch (_: IllegalStateException) { }
            assertEquals(old, repository.snapshotTrips())
            repository.replaceTrips(emptyList())
            assertTrue(repository.snapshotTrips().isEmpty())
            repository.replaceTrips(old)
            assertEquals(old, repository.snapshotTrips())
        } finally { database.close() }
    }

    @Test fun managerExportsInspectsAndRollsBackFailedRestore() = runBlocking {
        val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("backup_test_$name", mode)
        }
        val database = Room.inMemoryDatabaseBuilder(context, TabilineDatabase::class.java).build()
        val file = File.createTempFile("backup-test", ".tabiline", context.cacheDir)
        val settings = object : SettingsRepository {
            override val settings = MutableStateFlow(UserSettings())
            var failNext = false
            override suspend fun update(settings: UserSettings) {
                this.settings.value = settings
                if (failNext) { failNext = false; error("Simulated settings write failure") }
            }
        }
        try {
            val repo = OfflineTabilineRepository(database, database.dao())
            val backup = BackupManager(context, repo, settings)
            repo.replaceTrips(sample().trips)
            backup.export(Uri.fromFile(file))
            val exported = backup.inspect(Uri.fromFile(file))
            assertEquals(sample().trips, exported.trips)
            assertNotNull(backup.lastExportAt)
            val exportTime = backup.lastExportAt
            file.writeText("broken backup")
            try { backup.inspect(Uri.fromFile(file)); fail("Expected invalid-file rejection") }
            catch (_: Exception) { }
            assertEquals(exportTime, backup.lastExportAt)
            assertEquals(sample().trips, repo.snapshotTrips())
            val oldSettings = settings.settings.value
            settings.failNext = true
            try { backup.restore(sample().copy(trips = emptyList()), true); fail("Expected failure") }
            catch (_: IllegalStateException) { }
            assertEquals(sample().trips, repo.snapshotTrips())
            assertEquals(oldSettings, settings.settings.value)
            backup.restore(sample().copy(trips = emptyList()), false)
            assertTrue(repo.snapshotTrips().isEmpty())
            assertEquals(oldSettings, settings.settings.value)
            backup.restore(sample(), true)
            assertEquals(sample().settings, settings.settings.value)
            assertEquals(sample().trips, repo.snapshotTrips())
        } finally {
            file.delete(); database.close()
            context.getSharedPreferences("tabiline_backup", 0).edit().clear().commit()
        }
    }
}
