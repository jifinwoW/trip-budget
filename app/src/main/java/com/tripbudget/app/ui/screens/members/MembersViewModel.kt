package com.tripbudget.app.ui.screens.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.domain.MemberBalance
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.TripTotals
import com.tripbudget.app.ui.tripScopedStateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class MembersUiState(
    val isLoading: Boolean = true,
    val tripId: Long? = null,
    val balances: List<MemberBalance> = emptyList(),
    val contributions: List<ContributionEntity> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
)

class MembersViewModel(
    private val repository: TripRepository,
    tripIdFlow: Flow<Long?>,
) : ViewModel() {
    val uiState: StateFlow<MembersUiState> = tripScopedStateIn(
        tripIdFlow = tripIdFlow,
        loading = MembersUiState(isLoading = true),
        whenNoTrip = MembersUiState(isLoading = false),
    ) { tripId ->
        combine(
            repository.observeTripTotals(tripId),
            repository.observeContributions(tripId),
            repository.observeMembers(tripId),
        ) { totals: TripTotals?, contributions, members ->
            MembersUiState(
                isLoading = false,
                tripId = tripId,
                balances = totals?.memberBalances.orEmpty(),
                contributions = contributions,
                members = members,
            )
        }
    }

    fun addMember(
        tripId: Long,
        name: String,
        expectedRupees: String,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                onResult("Name is required")
                return@launch
            }
            val paise = Money.rupeesToPaise(expectedRupees)
            if (paise == null) {
                onResult("Enter a valid expected amount")
                return@launch
            }
            repository.insertMember(
                MemberEntity(
                    tripId = tripId,
                    name = trimmed,
                    expectedContributionPaise = paise,
                ),
            )
            onResult(null)
        }
    }

    fun updateMember(
        member: MemberEntity,
        name: String,
        expectedRupees: String,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                onResult("Name is required")
                return@launch
            }
            val paise = Money.rupeesToPaise(expectedRupees)
            if (paise == null) {
                onResult("Enter a valid expected amount")
                return@launch
            }
            repository.updateMember(
                member.copy(
                    name = trimmed,
                    expectedContributionPaise = paise,
                ),
            )
            onResult(null)
        }
    }

    fun deleteMember(member: MemberEntity) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun addContribution(
        tripId: Long,
        memberId: Long,
        amountRupees: String,
        note: String?,
        paidAt: Long,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val paise = Money.rupeesToPaise(amountRupees)
            if (paise == null || paise == 0L) {
                onResult("Enter a valid amount")
                return@launch
            }
            repository.insertContribution(
                ContributionEntity(
                    tripId = tripId,
                    memberId = memberId,
                    amountPaise = paise,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    paidAt = paidAt,
                ),
            )
            onResult(null)
        }
    }

    fun updateContribution(
        contribution: ContributionEntity,
        amountRupees: String,
        note: String?,
        paidAt: Long,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val paise = Money.rupeesToPaise(amountRupees)
            if (paise == null || paise == 0L) {
                onResult("Enter a valid amount")
                return@launch
            }
            repository.updateContribution(
                contribution.copy(
                    amountPaise = paise,
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    paidAt = paidAt,
                ),
            )
            onResult(null)
        }
    }

    fun deleteContribution(contribution: ContributionEntity) {
        viewModelScope.launch {
            repository.deleteContribution(contribution)
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
                    return MembersViewModel(repository, tripIdFlow) as T
                }
            }
    }
}
