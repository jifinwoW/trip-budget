package com.tripbudget.app.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.RouteStopEntity
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.places.PhotonPlaceSearch
import com.tripbudget.app.places.PlaceSuggestion
import com.tripbudget.app.ui.tripScopedStateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanUiState(
    val isLoading: Boolean = true,
    val tripId: Long? = null,
    val trip: TripEntity? = null,
    val stops: List<RouteStopEntity> = emptyList(),
    val placeQuery: String = "",
    val placeResults: List<PlaceSuggestion> = emptyList(),
    val placeSearching: Boolean = false,
    val placeError: String? = null,
)

@OptIn(FlowPreview::class)
class PlanViewModel(
    private val repository: TripRepository,
    tripIdFlow: Flow<Long?>,
    private val placeSearch: PhotonPlaceSearch = PhotonPlaceSearch(),
) : ViewModel() {
    private val placeQuery = MutableStateFlow("")
    private val placeResults = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    private val placeSearching = MutableStateFlow(false)
    private val placeError = MutableStateFlow<String?>(null)

    private val tripState: StateFlow<PlanUiState> = tripScopedStateIn(
        tripIdFlow = tripIdFlow,
        loading = PlanUiState(isLoading = true),
        whenNoTrip = PlanUiState(isLoading = false),
    ) { tripId ->
        combine(
            repository.observeTrip(tripId),
            repository.observeRouteStops(tripId),
        ) { trip, stops ->
            PlanUiState(
                isLoading = false,
                tripId = tripId,
                trip = trip,
                stops = stops,
            )
        }
    }

    val uiState: StateFlow<PlanUiState> = combine(
        tripState,
        placeQuery,
        placeResults,
        placeSearching,
        placeError,
    ) { trip, query, results, searching, error ->
        trip.copy(
            placeQuery = query,
            placeResults = results,
            placeSearching = searching,
            placeError = error,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PlanUiState(),
    )

    init {
        viewModelScope.launch {
            placeQuery.debounce(400).collect { q ->
                if (q.trim().length < 2) {
                    placeResults.value = emptyList()
                    placeSearching.value = false
                    placeError.value = null
                    return@collect
                }
                placeSearching.value = true
                placeError.value = null
                runCatching { placeSearch.search(q) }
                    .onSuccess { placeResults.value = it }
                    .onFailure { err ->
                        placeResults.value = emptyList()
                        placeError.value = err.message ?: "Search failed"
                    }
                placeSearching.value = false
            }
        }
    }

    fun onPlaceQueryChange(value: String) {
        placeQuery.value = value
    }

    fun clearPlaceSearch() {
        placeQuery.value = ""
        placeResults.value = emptyList()
        placeError.value = null
    }

    fun addPlace(suggestion: PlaceSuggestion) {
        val tripId = uiState.value.tripId ?: return
        viewModelScope.launch {
            repository.addRouteStopFromPlace(
                tripId = tripId,
                title = suggestion.title,
                subtitle = suggestion.subtitle,
                lat = suggestion.lat,
                lng = suggestion.lng,
                osmPlaceId = suggestion.osmPlaceId,
            )
            clearPlaceSearch()
        }
    }

    fun deleteStop(stop: RouteStopEntity) {
        viewModelScope.launch { repository.deleteRouteStop(stop) }
    }

    fun markReached(stop: RouteStopEntity) {
        viewModelScope.launch { repository.markRouteStopReached(stop.id) }
    }

    fun moveStop(stop: RouteStopEntity, direction: Int) {
        viewModelScope.launch { repository.moveRouteStop(stop.id, direction) }
    }

    fun setTrackingActive(active: Boolean, onDone: (() -> Unit)? = null) {
        val tripId = uiState.value.tripId ?: return
        viewModelScope.launch {
            repository.setRouteTrackingActive(tripId, active)
            onDone?.invoke()
        }
    }

    companion object {
        fun factory(
            repository: TripRepository,
            tripIdFlow: Flow<Long?>,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlanViewModel(repository, tripIdFlow) as T
                }
            }
    }
}
