package com.tripbudget.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripbudget.app.cloud.AuthRepository
import com.tripbudget.app.cloud.AuthUser
import com.tripbudget.app.cloud.SyncEngine
import com.tripbudget.app.cloud.friendlyCloudError
import com.tripbudget.app.data.TripCoverStore
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.places.TripPlace
import com.tripbudget.app.ui.screens.trips.TripCoverSelection
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TripSelectionUiState(
    val isLoading: Boolean = true,
    val trips: List<TripEntity> = emptyList(),
    /** Trips this account created (cloud owner). */
    val ownedTrips: List<TripEntity> = emptyList(),
    /** Trips joined via invite code. */
    val joinedTrips: List<TripEntity> = emptyList(),
    val selectedTripId: Long? = null,
    val selectedTrip: TripEntity? = null,
    val selectedIsOwned: Boolean = false,
)

class TripSelectionViewModel(
    private val appContext: Context,
    private val repository: TripRepository,
    private val syncEngine: SyncEngine,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val selectedTripId = MutableStateFlow<Long?>(null)

    // Eagerly keep auth hot so backgrounding for a few seconds does not reset to "signed out".
    val authUser: StateFlow<AuthUser?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isRefreshing: StateFlow<Boolean> = syncEngine.isRefreshing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<TripSelectionUiState> = combine(
        repository.observeTrips(),
        selectedTripId,
        authRepository.currentUser,
    ) { trips, selectedId, user ->
        val userId = user?.id
        val owned = trips.filter { isOwnedBy(it, userId) }
        val joined = trips.filter { !isOwnedBy(it, userId) }
        // Owned first so created trips stand out after joining others.
        val ordered = owned + joined
        val effectiveId = when {
            ordered.isEmpty() -> null
            selectedId != null && ordered.any { it.id == selectedId } -> selectedId
            else -> ordered.first().id
        }
        val selected = ordered.firstOrNull { it.id == effectiveId }
        TripSelectionUiState(
            isLoading = false,
            trips = ordered,
            ownedTrips = owned,
            joinedTrips = joined,
            selectedTripId = effectiveId,
            selectedTrip = selected,
            selectedIsOwned = selected != null && isOwnedBy(selected, userId),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TripSelectionUiState(isLoading = true),
    )

    fun hasEmailAccount(): Boolean = authRepository.hasEmailAccount()

    fun hasSession(): Boolean = authRepository.hasSession()

    fun isTripOwner(trip: TripEntity): Boolean =
        isOwnedBy(trip, authRepository.currentUserIdOrNull())

    fun refreshFromCloud(force: Boolean = false) {
        syncEngine.requestRefresh(force = force)
    }

    fun startLiveSync() {
        syncEngine.startRealtime()
        syncEngine.requestRefresh(force = true)
    }

    fun stopLiveSync() {
        syncEngine.stopRealtime()
    }

    fun selectTrip(tripId: Long) {
        selectedTripId.value = tripId
    }

    fun createTrip(
        name: String,
        place: TripPlace?,
        budgetRupees: String,
        startDate: LocalDate,
        endDate: LocalDate,
        cover: TripCoverSelection,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                onResult("Trip name is required")
                return@launch
            }
            if (endDate.isBefore(startDate)) {
                onResult("End date must be on or after start")
                return@launch
            }
            val paise = Money.rupeesToPaise(budgetRupees)
            if (paise == null) {
                onResult("Enter a valid budget amount")
                return@launch
            }
            val coverPath = resolveCover(existingPath = null, cover = cover)
            if (coverPath.isFailure) {
                onResult("Couldn't use that photo. Try another.")
                return@launch
            }
            val id = repository.insertTrip(
                TripEntity(
                    name = trimmedName,
                    destination = place?.name,
                    placeLat = place?.lat,
                    placeLng = place?.lng,
                    plannedBudgetPaise = paise,
                    startDateEpochDay = startDate.toEpochDay(),
                    endDateEpochDay = endDate.toEpochDay(),
                    remoteOwnerId = authRepository.currentUserIdOrNull(),
                    coverImagePath = coverPath.getOrNull(),
                ),
            )
            selectedTripId.value = id
            onResult(null)
        }
    }

    fun updateTrip(
        trip: TripEntity,
        name: String,
        place: TripPlace?,
        budgetRupees: String,
        startDate: LocalDate,
        endDate: LocalDate,
        cover: TripCoverSelection,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                onResult("Trip name is required")
                return@launch
            }
            if (endDate.isBefore(startDate)) {
                onResult("End date must be on or after start")
                return@launch
            }
            val paise = Money.rupeesToPaise(budgetRupees)
            if (paise == null) {
                onResult("Enter a valid budget amount")
                return@launch
            }
            val coverPath = resolveCover(existingPath = trip.coverImagePath, cover = cover)
            if (coverPath.isFailure) {
                onResult("Couldn't use that photo. Try another.")
                return@launch
            }
            repository.updateTrip(
                trip.copy(
                    name = trimmedName,
                    destination = place?.name,
                    placeLat = place?.lat,
                    placeLng = place?.lng,
                    plannedBudgetPaise = paise,
                    startDateEpochDay = startDate.toEpochDay(),
                    endDateEpochDay = endDate.toEpochDay(),
                    coverImagePath = coverPath.getOrNull(),
                    coverObjectKey = if (cover.pickedUri != null) null else trip.coverObjectKey,
                ),
            )
            onResult(null)
        }
    }

    fun deleteTrip(trip: TripEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            if (!isTripOwner(trip)) {
                onDone()
                return@launch
            }
            TripCoverStore.delete(trip.coverImagePath)
            repository.deleteTrip(trip)
            if (selectedTripId.value == trip.id) {
                selectedTripId.value = null
            }
            onDone()
        }
    }

    fun sendMemberNotification(
        trip: TripEntity,
        title: String,
        message: String,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val result = syncEngine.sendAnnouncement(trip.id, title, message)
            onResult(
                result.exceptionOrNull()?.let { err ->
                    friendlyCloudError(err, "Couldn't send that notification. Try again.")
                },
            )
        }
    }

    fun prepareShareCode(trip: TripEntity, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(syncEngine.ensureInviteCode(trip.id))
        }
    }

    fun joinTrip(code: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            // Join does not ask for email — use anonymous session if needed.
            val session = authRepository.ensureSessionForJoin()
            if (session.isFailure) {
                onResult(
                    friendlyCloudError(
                        session.exceptionOrNull() ?: IllegalStateException("Join failed"),
                        "Couldn't start the join. Try again.",
                    ),
                )
                return@launch
            }
            val result = syncEngine.joinTripByCode(code)
            result.fold(
                onSuccess = { onResult(null) },
                onFailure = { err ->
                    onResult(
                        friendlyCloudError(err, "Couldn't join that trip. Check the code and try again."),
                    )
                },
            )
        }
    }

    private suspend fun resolveCover(
        existingPath: String?,
        cover: TripCoverSelection,
    ): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            when {
                cover.pickedUri != null -> {
                    val saved = TripCoverStore.save(appContext, cover.pickedUri)
                        ?: error("unreadable")
                    if (saved != existingPath) TripCoverStore.delete(existingPath)
                    saved
                }
                cover.removeExisting -> {
                    TripCoverStore.delete(existingPath)
                    null
                }
                else -> existingPath
            }
        }
    }

    companion object {
        fun isOwnedBy(trip: TripEntity, userId: String?): Boolean {
            if (userId.isNullOrBlank()) return false
            val ownerId = trip.remoteOwnerId
            return when {
                ownerId != null -> ownerId == userId
                // Local create not pushed yet — creator is the current user.
                trip.remoteId == null -> true
                // Synced trip without owner metadata — treat as joined, not owned.
                else -> false
            }
        }

        fun factory(
            context: Context,
            repository: TripRepository,
            syncEngine: SyncEngine,
            authRepository: AuthRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TripSelectionViewModel(
                        context.applicationContext,
                        repository,
                        syncEngine,
                        authRepository,
                    ) as T
                }
            }
    }
}
