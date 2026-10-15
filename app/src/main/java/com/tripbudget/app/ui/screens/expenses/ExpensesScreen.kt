package com.tripbudget.app.ui.screens.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.ui.components.ConfirmDialog
import com.tripbudget.app.ui.components.EmptyState
import com.tripbudget.app.ui.components.LedgerAtmosphere
import com.tripbudget.app.ui.components.LedgerRule
import com.tripbudget.app.ui.components.LoadingContent
import java.time.LocalDate

@Composable
fun ExpensesScreen(
    tripId: Long,
    viewModel: ExpensesViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExpenseEntity?>(null) }
    var deleting by remember { mutableStateOf<ExpenseEntity?>(null) }

    val dataReady = !state.isLoading && state.tripId == tripId
    if (!dataReady) {
        LoadingContent()
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        },
    ) { padding ->
        if (state.expenses.isEmpty()) {
            EmptyState(
                title = "No expenses yet",
                message = "Record food, fuel, stays, and other trip costs. Shared-fund expenses do not need a payer.",
                actionLabel = "Add expense",
                onAction = { showAdd = true },
                modifier = Modifier.padding(padding),
            )
        } else {
            LedgerAtmosphere(modifier = Modifier.padding(padding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                ) {
                    item {
                        Text(
                            text = "EXPENSE LEDGER",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    items(state.expenses, key = { it.id }) { expense ->
                        val payer = expense.paidByMemberId?.let { id ->
                            state.members.firstOrNull { it.id == id }?.name
                        }
                        ExpenseRow(
                            expense = expense,
                            payerName = payer,
                            onEdit = { editing = expense },
                            onDelete = { deleting = expense },
                        )
                        LedgerRule()
                    }
                }
            }
        }
    }

    if (showAdd) {
        ExpenseEditorDialog(
            members = state.members,
            existing = null,
            onDismiss = { showAdd = false },
            onSave = { title, amount, category, date, paidBy, source, notes, onResult ->
                viewModel.saveExpense(
                    existing = null,
                    tripId = tripId,
                    title = title,
                    amountRupees = amount,
                    category = category,
                    date = date,
                    paidByMemberId = paidBy,
                    paymentSource = source,
                    notes = notes,
                    onResult = onResult,
                )
            },
        )
    }
    editing?.let { expense ->
        ExpenseEditorDialog(
            members = state.members,
            existing = expense,
            onDismiss = { editing = null },
            onSave = { title, amount, category, date, paidBy, source, notes, onResult ->
                viewModel.saveExpense(
                    existing = expense,
                    tripId = tripId,
                    title = title,
                    amountRupees = amount,
                    category = category,
                    date = date,
                    paidByMemberId = paidBy,
                    paymentSource = source,
                    notes = notes,
                    onResult = onResult,
                )
            },
        )
    }
    deleting?.let { expense ->
        ConfirmDialog(
            title = "Delete expense?",
            message = "Remove \"${expense.title}\" (${Money.formatInr(expense.amountPaise)})?",
            onConfirm = {
                viewModel.deleteExpense(expense)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun ExpenseRow(
    expense: ExpenseEntity,
    payerName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val sourceLabel = when (expense.paymentSource) {
        PaymentSource.SHARED -> "Shared fund"
        PaymentSource.PERSONAL -> if (payerName != null) "Personal · Paid by $payerName" else "Personal"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(expense.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${expense.category.label.uppercase()} · $sourceLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = LocalDate.ofEpochDay(expense.dateEpochDay).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = Money.formatInr(expense.amountPaise),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 2.dp, end = 4.dp),
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}
