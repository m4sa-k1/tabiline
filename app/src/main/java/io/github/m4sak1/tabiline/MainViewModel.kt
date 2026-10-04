package io.github.m4sak1.tabiline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.Trip
import io.github.m4sak1.tabiline.core.model.TripWithLegs
import io.github.m4sak1.tabiline.core.model.UserSettings
import io.github.m4sak1.tabiline.data.repository.TabilineRepository
import io.github.m4sak1.tabiline.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: TabilineRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val trips: StateFlow<List<TripWithLegs>> = repository.observeTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun observeTrip(id: Long) = repository.observeTrip(id)
    suspend fun getLeg(id: Long) = repository.getLeg(id)

    fun saveTrip(trip: Trip, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        onSaved(repository.saveTrip(trip))
    }

    fun deleteTrip(id: Long, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.deleteTrip(id); onDone()
    }

    fun saveLeg(leg: TransportLeg, onSaved: () -> Unit = {}) = viewModelScope.launch {
        repository.saveLeg(leg); onSaved()
    }

    fun deleteLeg(id: Long, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.deleteLeg(id); onDone()
    }

    fun reorder(tripId: Long, ids: List<Long>) = viewModelScope.launch {
        repository.reorderLegs(tripId, ids)
    }

    fun updateSettings(value: UserSettings) = viewModelScope.launch { settingsRepository.update(value) }

    class Factory(private val container: io.github.m4sak1.tabiline.di.AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(container.trips, container.settings) as T
    }
}
