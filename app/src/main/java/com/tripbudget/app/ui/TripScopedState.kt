package com.tripbudget.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Trip-scoped UI state helper.
 *
 * Always start with [loading] so screens never flash an empty/"no trip" state
 * before Room emits. Use [whenNoTrip] only after a real null trip id emission.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> ViewModel.tripScopedStateIn(
    tripIdFlow: Flow<Long?>,
    loading: T,
    whenNoTrip: T,
    transform: (tripId: Long) -> Flow<T>,
): StateFlow<T> {
    return tripIdFlow
        .flatMapLatest { tripId ->
            if (tripId == null) {
                flowOf(whenNoTrip)
            } else {
                transform(tripId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            // Eager: start loading as soon as the ViewModel exists (shell already has a trip).
            started = SharingStarted.Eagerly,
            initialValue = loading,
        )
}
