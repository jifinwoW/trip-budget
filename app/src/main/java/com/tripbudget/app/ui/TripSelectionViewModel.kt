package com.tripbudget.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripSelectionUiState(
    val isLoading: Boolean = true,
    val trips: List<TripEntity> = emptyList(),
    val selectedTripId: Long? = null,
    val selectedTrip: TripEntity? = null,
)

class TripSelectionViewModel(
    private val repository: TripRepository,
) : ViewModel() {
    private val selectedTripId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<TripSelectionUiState> = combine(
        repository.observeTrips(),
        selectedTripId,
    ) { trips, selectedId ->
        val effectiveId = when {
            trips.isEmpty() -> null
            selectedId != null && trips.any { it.id == selectedId } -> selectedId
            else -> trips.first().id
        }
        TripSelectionUiState(
            isLoading = false,
            trips = trips,
            selectedTripId = effectiveId,
            selectedTrip = trips.firstOrNull { it.id == effectiveId },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TripSelectionUiState(isLoading = true),
    )

    fun selectTrip(tripId: Long) {
        selectedTripId.value = tripId
    }

    fun createTrip(
        name: String,
        destination: String?,
        budgetRupees: String,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                onResult("Trip name is required")
                return@launch
            }
            val paise = Money.rupeesToPaise(budgetRupees)
            if (paise == null) {
                onResult("Enter a valid budget amount")
                return@launch
            }
            val id = repository.insertTrip(
                TripEntity(
                    name = trimmedName,
                    destination = destination?.trim()?.takeIf { it.isNotEmpty() },
                    plannedBudgetPaise = paise,
                ),
            )
            selectedTripId.value = id
            onResult(null)
        }
    }

    fun updateTrip(
        trip: TripEntity,
        name: String,
        destination: String?,
        budgetRupees: String,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                onResult("Trip name is required")
                return@launch
            }
            val paise = Money.rupeesToPaise(budgetRupees)
            if (paise == null) {
                onResult("Enter a valid budget amount")
                return@launch
            }
            repository.updateTrip(
                trip.copy(
                    name = trimmedName,
                    destination = destination?.trim()?.takeIf { it.isNotEmpty() },
                    plannedBudgetPaise = paise,
                ),
            )
            onResult(null)
        }
    }

    fun deleteTrip(trip: TripEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
            if (selectedTripId.value == trip.id) {
                selectedTripId.value = null
            }
            onDone()
        }
    }

    companion object {
        fun factory(repository: TripRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TripSelectionViewModel(repository) as T
                }
            }
    }
}
