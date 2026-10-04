package io.github.m4sak1.tabiline.di

import android.content.Context
import androidx.room.Room
import io.github.m4sak1.tabiline.data.local.TabilineDatabase
import io.github.m4sak1.tabiline.data.repository.OfflineTabilineRepository
import io.github.m4sak1.tabiline.data.repository.TabilineRepository
import io.github.m4sak1.tabiline.data.settings.DataStoreSettingsRepository
import io.github.m4sak1.tabiline.data.settings.SettingsRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        TabilineDatabase::class.java,
        "tabiline.db",
    ).build()

    val trips: TabilineRepository = OfflineTabilineRepository(database, database.dao())
    val settings: SettingsRepository = DataStoreSettingsRepository(context)
}
