package io.github.m4sak1.tabiline.di

import android.content.Context
import androidx.room.Room
import io.github.m4sak1.tabiline.data.local.TabilineDatabase
import io.github.m4sak1.tabiline.data.local.SampleDataSeeder
import io.github.m4sak1.tabiline.data.repository.OfflineTabilineRepository
import io.github.m4sak1.tabiline.data.repository.TabilineRepository
import io.github.m4sak1.tabiline.data.settings.DataStoreSettingsRepository
import io.github.m4sak1.tabiline.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import io.github.m4sak1.tabiline.notifications.DepartureReminders

class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = Room.databaseBuilder(
        context,
        TabilineDatabase::class.java,
        "tabiline.db",
    ).addMigrations(
        TabilineDatabase.MIGRATION_1_2,
        TabilineDatabase.MIGRATION_2_3,
        TabilineDatabase.MIGRATION_3_4,
        TabilineDatabase.MIGRATION_4_5,
    ).build()

    val trips: TabilineRepository = OfflineTabilineRepository(database, database.dao())
    val settings: SettingsRepository = DataStoreSettingsRepository(context)
    val reminders = DepartureReminders(context.applicationContext)

    fun refreshReminders() {
        applicationScope.launch { reminders.sync(trips.observeTrips().first(), settings.settings.first()) }
    }

    init {
        applicationScope.launch { SampleDataSeeder(context, database).seedIfNeeded() }
        applicationScope.launch {
            combine(trips.observeTrips(), settings.settings) { trips, settings -> trips to settings }
                .collect { (trips, settings) -> reminders.sync(trips, settings) }
        }
    }
}
