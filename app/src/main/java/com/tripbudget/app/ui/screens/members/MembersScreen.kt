package com.tripbudget.app.ui.screens.members

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.domain.MemberBalance
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.components.ConfirmDialog
import com.tripbudget.app.ui.components.EmptyState
import com.tripbudget.app.ui.components.LedgerAtmosphere
import com.tripbudget.app.ui.components.LedgerRule
import com.tripbudget.app.ui.components.LoadingContent
import com.tripbudget.app.ui.theme.LedgerTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    tripId: Long,
    viewModel: MembersViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddMember by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<MemberEntity?>(null) }
    var deletingMember by remember { mutableStateOf<MemberEntity?>(null) }
    var contributionForMember by remember { mutableStateOf<MemberEntity?>(null) }
    var editingContribution by remember { mutableStateOf<ContributionEntity?>(null) }
    var deletingContribution by remember { mutableStateOf<ContributionEntity?>(null) }
    var historyMemberId by remember { mutableStateOf<Long?>(null) }

    val dataReady = !state.isLoading && state.tripId == tripId
    if (!dataReady) {
        LoadingContent()
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMember = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add member")
            }
        },
    ) { padding ->
        if (state.balances.isEmpty()) {
            EmptyState(
                title = "No members yet",
                message = "Add people and set how much each should contribute.",
                actionLabel = "Add member",
                onAction = { showAddMember = true },
                modifier = Modifier.padding(padding),
            )
        } else {
            LedgerAtmosphere(modifier = Modifier.padding(padding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item {
                        Text(
                            text = "CREW & CONTRIBUTIONS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    items(state.balances, key = { it.memberId }) { balance ->
                        val member = state.members.firstOrNull { it.id == balance.memberId }
                        MemberRow(
                            balance = balance,
                            onEdit = { member?.let { editingMember = it } },
                            onDelete = { member?.let { deletingMember = it } },
                            onAddPayment = { member?.let { contributionForMember = it } },
                            onToggleHistory = {
                                historyMemberId =
                                    if (historyMemberId == balance.memberId) null else balance.memberId
                            },
                            historyExpanded = historyMemberId == balance.memberId,
                        )
                        if (historyMemberId == balance.memberId) {
                            val history = state.contributions.filter { it.memberId == balance.memberId }
                            if (history.isEmpty()) {
                                Text(
                                    text = "No payments recorded.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                                )
                            } else {
                                history.forEach { contribution ->
                                    ContributionHistoryRow(
                                        contribution = contribution,
                                        onEdit = { editingContribution = contribution },
                                        onDelete = { deletingContribution = contribution },
                                    )
                                }
                            }
                        }
                        LedgerRule()
                    }
                }
            }
        }
    }

    if (showAddMember) {
        MemberEditorDialog(
            existing = null,
            onDismiss = { showAddMember = false },
            onSave = { name, expected, onResult ->
                viewModel.addMember(tripId, name, expected, onResult)
            },
        )
    }
    editingMember?.let { member ->
        MemberEditorDialog(
            existing = member,
            onDismiss = { editingMember = null },
            onSave = { name, expected, onResult ->
                viewModel.updateMember(member, name, expected, onResult)
            },
        )
    }
    contributionForMember?.let { member ->
        ContributionEditorDialog(
            members = listOf(member),
            existing = null,
            defaultMemberId = member.id,
            onDismiss = { contributionForMember = null },
            onSave = { memberId, amount, note, onResult ->
                viewModel.addContribution(tripId, memberId, amount, note, onResult)
            },
        )
    }
    editingContribution?.let { contribution ->
        ContributionEditorDialog(
            members = state.members,
            existing = contribution,
            defaultMemberId = contribution.memberId,
            onDismiss = { editingContribution = null },
            onSave = { _, amount, note, onResult ->
                viewModel.updateContribution(contribution, amount, note, onResult)
            },
        )
    }
    deletingMember?.let { member ->
        ConfirmDialog(
            title = "Delete member?",
            message = "This removes ${member.name} and their payment history.",
            onConfirm = {
                viewModel.deleteMember(member)
                deletingMember = null
            },
            onDismiss = { deletingMember = null },
        )
    }
    deletingContribution?.let { contribution ->
        ConfirmDialog(
            title = "Delete payment?",
            message = "Remove this contribution of ${Money.formatInr(contribution.amountPaise)}?",
            onConfirm = {
                viewModel.deleteContribution(contribution)
                deletingContribution = null
            },
            onDismiss = { deletingContribution = null },
        )
    }
}

@Composable
private fun MemberRow(
    balance: MemberBalance,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddPayment: () -> Unit,
    onToggleHistory: () -> Unit,
    historyExpanded: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f).clickable(onClick = onToggleHistory)) {
                Text(balance.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Expected ${Money.formatInr(balance.expectedPaise)} · Paid ${Money.formatInr(balance.paidPaise)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val balanceText = if (balance.hasExcess) {
                    "Excess ${Money.formatInr(balance.excessPaise)}"
                } else {
                    "Outstanding ${Money.formatInr(balance.outstandingPaise)}"
                }
                Text(
                    text = balanceText,
                    style = MaterialTheme.typography.titleLarge,
                    color = when {
                        balance.hasExcess -> LedgerTokens.positive
                        balance.outstandingPaise > 0 -> LedgerTokens.negative
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = if (historyExpanded) "HIDE HISTORY" else "PAYMENT HISTORY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(onClick = onAddPayment) {
                Icon(Icons.Default.Payments, contentDescription = "Record payment")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit member")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete member")
            }
        }
    }
}

@Composable
private fun ContributionHistoryRow(
    contribution: ContributionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy")
            .withZone(ZoneId.systemDefault())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(Money.formatInr(contribution.amountPaise), style = MaterialTheme.typography.titleMedium)
            Text(
                text = formatter.format(Instant.ofEpochMilli(contribution.paidAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            contribution.note?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        TextButton(onClick = onEdit) { Text("Edit") }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}
