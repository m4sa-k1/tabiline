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

    init {
        applicationScope.launch { SampleDataSeeder(context, database).seedIfNeeded() }
    }
}
