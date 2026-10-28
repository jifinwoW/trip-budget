package com.tripbudget.app.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
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
        ) { expenses, members ->
            ExpensesUiState(
                isLoading = false,
                tripId = tripId,
                expenses = expenses,
                members = members,
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
