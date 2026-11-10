package com.tripbudget.app.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.data.entity.PlannedExpenseEntity
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.ui.tripScopedStateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExpensesUiState(
    val isLoading: Boolean = true,
    val tripId: Long? = null,
    val expenses: List<ExpenseEntity> = emptyList(),
    val members: List<MemberEntity> = emptyList(),
    val plannedExpenses: List<PlannedExpenseEntity> = emptyList(),
    val tripPlannedBudgetPaise: Long = 0L,
)

class ExpensesViewModel(
    private val repository: TripRepository,
    tripIdFlow: Flow<Long?>,
) : ViewModel() {
    val uiState: StateFlow<ExpensesUiState> = tripScopedStateIn(
        tripIdFlow = tripIdFlow,
        loading = ExpensesUiState(isLoading = true),
        whenNoTrip = ExpensesUiState(isLoading = false),
    ) { tripId ->
        combine(
            repository.observeExpenses(tripId),
            repository.observeMembers(tripId),
            repository.observePlannedExpenses(tripId),
            repository.observeTrip(tripId),
        ) { expenses, members, planned, trip ->
            ExpensesUiState(
                isLoading = false,
                tripId = tripId,
                expenses = expenses,
                members = members,
                plannedExpenses = planned,
                tripPlannedBudgetPaise = trip?.plannedBudgetPaise ?: 0L,
            )
        }
    }

    fun saveExpense(
        existing: ExpenseEntity?,
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
            val entity = ExpenseEntity(
                id = existing?.id ?: 0,
                tripId = tripId,
                title = trimmed,
                amountPaise = paise,
                category = category,
                dateEpochDay = date.toEpochDay(),
                paidByMemberId = if (paymentSource == PaymentSource.PERSONAL) paidByMemberId else null,
                paymentSource = paymentSource,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                remoteId = existing?.remoteId,
            )
            if (existing == null) {
                repository.insertExpense(entity)
            } else {
                repository.updateExpense(entity)
            }
            onResult(null)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun savePlannedExpense(
        existing: PlannedExpenseEntity?,
        tripId: Long,
        title: String,
        amountRupees: String,
        category: ExpenseCategory,
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
            if (paise == null || paise <= 0L) {
                onResult("Enter a valid amount")
                return@launch
            }
            val entity = PlannedExpenseEntity(
                id = existing?.id ?: 0,
                tripId = tripId,
                title = trimmed,
                amountPaise = paise,
                category = category,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                remoteId = existing?.remoteId,
            )
            if (existing == null) {
                repository.insertPlannedExpense(entity)
            } else {
                repository.updatePlannedExpense(entity)
            }
            onResult(null)
        }
    }

    fun deletePlannedExpense(item: PlannedExpenseEntity) {
        viewModelScope.launch {
            repository.deletePlannedExpense(item)
        }
    }

    fun syncTripBudgetWithPlannedTotal(tripId: Long, totalPlannedPaise: Long) {
        viewModelScope.launch {
            val trip = repository.getTrip(tripId) ?: return@launch
            repository.updateTrip(trip.copy(plannedBudgetPaise = totalPlannedPaise))
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
                    return ExpensesViewModel(repository, tripIdFlow) as T
                }
            }
    }
}
