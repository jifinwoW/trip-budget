package com.tripbudget.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.ui.components.EnterCascade
import com.tripbudget.app.ui.components.LedgerAtmosphere
import com.tripbudget.app.ui.components.LedgerRule
import com.tripbudget.app.ui.components.LoadingContent
import com.tripbudget.app.ui.components.MetricCell
import com.tripbudget.app.ui.screens.expenses.ExpenseEditorDialog
import com.tripbudget.app.ui.screens.members.ContributionEditorDialog
import com.tripbudget.app.ui.theme.LedgerTokens
import java.time.LocalDate

@Composable
fun DashboardScreen(
    trip: TripEntity,
    viewModel: DashboardViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddContribution by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }

    val totals = state.totals
    val totalsReady = !state.isLoading && state.trip?.id == trip.id && totals != null

    if (!totalsReady || totals == null) {
        LedgerAtmosphere {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = (trip.destination?.takeIf { it.isNotBlank() } ?: "TRIP LEDGER").uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = trip.name,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
                LoadingContent(modifier = Modifier.weight(1f))
            }
        }
        return
    }

    val sharedColor = if (totals.sharedCashRemainingPaise < 0) {
        LedgerTokens.negative
    } else {
        MaterialTheme.colorScheme.primary
    }
    val budgetColor = if (totals.isOverBudget) {
        LedgerTokens.negative
    } else {
        MaterialTheme.colorScheme.primary
    }

    LedgerAtmosphere {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            EnterCascade(0) {
                Text(
                    text = (trip.destination?.takeIf { it.isNotBlank() } ?: "TRIP LEDGER").uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (totals.isOverBudget) {
                Spacer(Modifier.height(16.dp))
                EnterCascade(40) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.shapes.small,
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                                MaterialTheme.shapes.small,
                            )
                            .padding(14.dp),
                    ) {
                        Text(
                            text = "OVER BUDGET",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = Money.formatInr(-totals.budgetRemainingPaise) + " over plan",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            EnterCascade(80) {
                Text(
                    text = "SHARED CASH",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Money.formatInr(totals.sharedCashRemainingPaise),
                    style = MaterialTheme.typography.displayLarge,
                    color = sharedColor,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = "Collected minus shared-fund spend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            EnterCascade(120) {
                LedgerRule()
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(
                        label = "Budget left",
                        amountPaise = totals.budgetRemainingPaise,
                        amountColor = budgetColor,
                        modifier = Modifier.weight(1f),
                    )
                    MetricCell(
                        label = "Outstanding",
                        amountPaise = totals.outstandingContributionsPaise,
                        modifier = Modifier.weight(1f),
                    )
                }
                LedgerRule()
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(
                        label = "Planned",
                        amountPaise = totals.plannedBudgetPaise,
                        modifier = Modifier.weight(1f),
                    )
                    MetricCell(
                        label = "Collected",
                        amountPaise = totals.contributionsCollectedPaise,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(
                        label = "Spent",
                        amountPaise = totals.totalExpensesPaise,
                        modifier = Modifier.weight(1f),
                    )
                    MetricCell(
                        label = "Members due",
                        amountPaise = totals.outstandingContributionsPaise,
                        modifier = Modifier.weight(1f),
                    )
                }
                LedgerRule()
            }

            Spacer(Modifier.height(12.dp))
            EnterCascade(160) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = { showAddContribution = true },
                        enabled = state.members.isNotEmpty(),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            "CONTRIBUTE",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = { showAddExpense = true },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            "EXPENSE",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
                if (state.members.isEmpty()) {
                    Text(
                        text = "Add members to record contributions or personal expenses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            EnterCascade(200) {
                Text(
                    text = "RECENT LEDGER",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (state.recentExpenses.isEmpty()) {
                    Text(
                        text = "No expenses recorded yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    state.recentExpenses.forEach { expense ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(expense.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${expense.category.label} · ${
                                        if (expense.paymentSource == PaymentSource.SHARED) "Shared" else "Personal"
                                    } · ${LocalDate.ofEpochDay(expense.dateEpochDay)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = Money.formatInr(expense.amountPaise),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        LedgerRule()
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddContribution) {
        ContributionEditorDialog(
            members = state.members,
            existing = null,
            defaultMemberId = state.members.firstOrNull()?.id,
            onDismiss = { showAddContribution = false },
            onSave = { memberId, amount, note, onResult ->
                viewModel.addContribution(trip.id, memberId, amount, note, onResult)
            },
        )
    }

    if (showAddExpense) {
        ExpenseEditorDialog(
            members = state.members,
            existing = null,
            onDismiss = { showAddExpense = false },
            onSave = { title, amount, category, date, paidBy, source, notes, onResult ->
                viewModel.addExpense(
                    tripId = trip.id,
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
}
