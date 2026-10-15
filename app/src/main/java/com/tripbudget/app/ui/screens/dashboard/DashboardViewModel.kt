package com.tripbudget.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.domain.TripTotals
import com.tripbudget.app.ui.tripScopedStateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val isLoading: Boolean = true,
    val trip: TripEntity? = null,
    val totals: TripTotals? = null,
    val recentExpenses: List<ExpenseEntity> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
)

class DashboardViewModel(
    private val repository: TripRepository,
    tripIdFlow: Flow<Long?>,
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = tripScopedStateIn(
        tripIdFlow = tripIdFlow,
        loading = DashboardUiState(isLoading = true),
        whenNoTrip = DashboardUiState(isLoading = false),
    ) { tripId ->
        combine(
            repository.observeTrip(tripId),
            repository.observeTripTotals(tripId),
            repository.observeRecentExpenses(tripId),
            repository.observeMembers(tripId),
        ) { trip, totals, recent, members ->
            DashboardUiState(
                isLoading = false,
                trip = trip,
                totals = totals,
                recentExpenses = recent,
                members = members,
            )
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

    fun addExpense(
        tripId: Long,
        title: String,
        amountRupees: String,
        category: ExpenseCategory,
        date: LocalDate,
        paidByMemberId: Long?,
        paymentSource: PaymentSource,
        notes: String?,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) {
                onResult("Title is required")
                return@launch
            }
            val paise = Money.rupeesToPaise(amountRupees)
            if (paise == null || paise == 0L) {
                onResult("Enter a valid amount")
                return@launch
            }
            if (paymentSource == PaymentSource.PERSONAL && paidByMemberId == null) {
                onResult("Select who paid")
                return@launch
            }
            repository.insertExpense(
                ExpenseEntity(
                    tripId = tripId,
                    title = trimmed,
                    amountPaise = paise,
                    category = category,
                    dateEpochDay = date.toEpochDay(),
                    paidByMemberId = if (paymentSource == PaymentSource.PERSONAL) paidByMemberId else null,
                    paymentSource = paymentSource,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
            onResult(null)
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
                    return DashboardViewModel(repository, tripIdFlow) as T
                }
            }
    }
}
