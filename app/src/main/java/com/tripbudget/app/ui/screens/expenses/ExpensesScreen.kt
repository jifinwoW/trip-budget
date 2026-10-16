package com.tripbudget.app.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.ui.components.CategoryIcon
import com.tripbudget.app.ui.components.ConfirmDialog
import com.tripbudget.app.ui.components.EmptyState
import com.tripbudget.app.ui.components.LivePullRefresh
import com.tripbudget.app.ui.components.LoadingContent
import com.tripbudget.app.ui.components.SoftScreenCanvas
import com.tripbudget.app.ui.theme.CoastalTokens
import com.tripbudget.app.ui.theme.Teal600
import com.tripbudget.app.ui.theme.floatingNavContentPadding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class ExpenseFilter { ALL, SHARED, PERSONAL }

@Composable
fun ExpensesScreen(
    tripId: Long,
    viewModel: ExpensesViewModel,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExpenseEntity?>(null) }
    var deleting by remember { mutableStateOf<ExpenseEntity?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ExpenseFilter.ALL) }

    val dataReady = !state.isLoading && state.tripId == tripId
    if (!dataReady) {
        LoadingContent()
        return
    }

    val filtered = remember(state.expenses, query, filter) {
        state.expenses
            .asSequence()
            .filter { expense ->
                when (filter) {
                    ExpenseFilter.ALL -> true
                    ExpenseFilter.SHARED -> expense.paymentSource == PaymentSource.SHARED
                    ExpenseFilter.PERSONAL -> expense.paymentSource == PaymentSource.PERSONAL
                }
            }
            .filter { expense ->
                if (query.isBlank()) true
                else {
                    val q = query.trim().lowercase()
                    expense.title.lowercase().contains(q) ||
                        expense.category.label.lowercase().contains(q) ||
                        (expense.notes?.lowercase()?.contains(q) == true)
                }
            }
            .toList()
    }

    val grouped = remember(filtered) {
        filtered
            .groupBy { it.dateEpochDay }
            .toList()
            .sortedByDescending { it.first }
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = Teal600,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(bottom = floatingNavContentPadding()),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        },
    ) { padding ->
        LivePullRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding),
        ) {
        if (state.expenses.isEmpty()) {
            EmptyState(
                title = "No expenses yet",
                message = "Record food, fuel, stays, and other trip costs.",
                actionLabel = "Add expense",
                onAction = { showAdd = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SoftScreenCanvas {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = floatingNavContentPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text("Expenses", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Track shared and personal spend",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search expenses") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, tint = CoastalTokens.tealSoftFg)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CoastalTokens.card,
                                unfocusedContainerColor = CoastalTokens.card,
                                focusedBorderColor = Teal600,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        ) {
                            ExpenseFilter.entries.forEach { option ->
                                val selected = filter == option
                                FilterChip(
                                    selected = selected,
                                    onClick = { filter = option },
                                    label = {
                                        Text(
                                            when (option) {
                                                ExpenseFilter.ALL -> "All"
                                                ExpenseFilter.SHARED -> "Shared"
                                                ExpenseFilter.PERSONAL -> "Personal"
                                            },
                                        )
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CoastalTokens.tealSoft,
                                        selectedLabelColor = CoastalTokens.tealSoftFg,
                                        containerColor = CoastalTokens.card,
                                    ),
                                )
                            }
                        }
                    }

                    if (grouped.isEmpty()) {
                        item {
                            Text(
                                "No expenses match your search.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    } else {
                        grouped.forEach { (epochDay, dayExpenses) ->
                            val dayTotal = dayExpenses.sumOf { it.amountPaise }
                            item(key = "header-$epochDay") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        LocalDate.ofEpochDay(epochDay).format(dateFmt),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CoastalTokens.tealSoftFg,
                                    )
                                    Text(
                                        Money.formatInr(dayTotal),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            items(dayExpenses, key = { it.id }) { expense ->
                                val payer = expense.paidByMemberId?.let { id ->
                                    state.members.firstOrNull { it.id == id }?.name
                                }
                                ExpenseShowcaseRow(
                                    expense = expense,
                                    payerName = payer,
                                    onClick = { editing = expense },
                                    onEdit = { editing = expense },
                                    onDelete = { deleting = expense },
                                )
                            }
                        }
                    }
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
private fun ExpenseShowcaseRow(
    expense: ExpenseEntity,
    payerName: String?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val source = when (expense.paymentSource) {
        PaymentSource.SHARED -> "Shared"
        PaymentSource.PERSONAL -> if (payerName != null) "Personal · $payerName" else "Personal"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(CoastalTokens.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(expense.category)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(expense.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${expense.category.label} · $source",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "−${Money.formatInr(expense.amountPaise)}",
                style = MaterialTheme.typography.titleLarge,
                color = CoastalTokens.ink,
            )
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
