package com.tripbudget.app.ui.screens.members

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.domain.MemberBalance
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.components.CoastalProgressTrack
import com.tripbudget.app.ui.components.ConfirmDialog
import com.tripbudget.app.ui.components.EmptyState
import com.tripbudget.app.ui.components.LivePullRefresh
import com.tripbudget.app.ui.components.ListShimmer
import com.tripbudget.app.ui.components.MemberAvatar
import com.tripbudget.app.ui.components.MemberStatusBadge
import com.tripbudget.app.ui.components.SectionHeader
import com.tripbudget.app.ui.components.SoftScreenCanvas
import com.tripbudget.app.ui.components.memberPayStatus
import com.tripbudget.app.ui.theme.CoastalTokens
import com.tripbudget.app.ui.theme.Teal600
import com.tripbudget.app.ui.theme.floatingNavContentPadding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MembersScreen(
    tripId: Long,
    viewModel: MembersViewModel,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onPullProgress: (fraction: Float, tracking: Boolean) -> Unit = { _, _ -> },
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
        ListShimmer(rowHeight = 120.dp)
        return
    }

    val expectedTotal = state.balances.sumOf { it.expectedPaise }
    val collectedTotal = state.balances.sumOf { it.paidPaise }
    val collectedProgress = if (expectedTotal > 0) {
        (collectedTotal.toFloat() / expectedTotal.toFloat()).coerceAtLeast(0f)
    } else {
        0f
    }

    Scaffold(
        containerColor = Color.Transparent,
        // Outer nav host already handles status bar + app header; don't double-pad top.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMember = true },
                containerColor = Teal600,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(bottom = floatingNavContentPadding()),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add member")
            }
        },
    ) { padding ->
        LivePullRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onPullProgress = onPullProgress,
            modifier = Modifier.padding(padding),
        ) {
        if (state.balances.isEmpty()) {
            EmptyState(
                title = "No members yet",
                message = "Add people and set how much each should contribute.",
                actionLabel = "Add member",
                onAction = { showAddMember = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SoftScreenCanvas {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 22.dp,
                        bottom = floatingNavContentPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        SectionHeader(
                            title = "Members",
                            subtitle = "Contributions and balances",
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(18.dp), clip = false)
                                .clip(RoundedCornerShape(18.dp))
                                .background(CoastalTokens.card)
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Contributions collected",
                                style = MaterialTheme.typography.labelLarge,
                                color = CoastalTokens.muted,
                            )
                            Text(
                                Money.formatInr(collectedTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                color = CoastalTokens.tealSoftFg,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "of ${Money.formatInr(expectedTotal)} expected · ${(collectedProgress * 100f).toInt().coerceAtLeast(0)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoastalTokens.muted,
                            )
                            Spacer(Modifier.height(4.dp))
                            CoastalProgressTrack(
                                progress = collectedProgress,
                            )
                        }
                    }
                    items(state.balances, key = { it.memberId }) { balance ->
                        val member = state.members.firstOrNull { it.id == balance.memberId }
                        MemberCard(
                            balance = balance,
                            historyExpanded = historyMemberId == balance.memberId,
                            history = state.contributions.filter { it.memberId == balance.memberId },
                            onEdit = { member?.let { editingMember = it } },
                            onDelete = { member?.let { deletingMember = it } },
                            onAddPayment = { member?.let { contributionForMember = it } },
                            onToggleHistory = {
                                historyMemberId =
                                    if (historyMemberId == balance.memberId) null else balance.memberId
                            },
                            onEditContribution = { editingContribution = it },
                            onDeleteContribution = { deletingContribution = it },
                        )
                    }
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
            onSave = { memberId, amount, note, paidAt, onResult ->
                viewModel.addContribution(tripId, memberId, amount, note, paidAt, onResult)
            },
        )
    }
    editingContribution?.let { contribution ->
        ContributionEditorDialog(
            members = state.members,
            existing = contribution,
            defaultMemberId = contribution.memberId,
            onDismiss = { editingContribution = null },
            onSave = { _, amount, note, paidAt, onResult ->
                viewModel.updateContribution(contribution, amount, note, paidAt, onResult)
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
private fun MemberCard(
    balance: MemberBalance,
    historyExpanded: Boolean,
    history: List<ContributionEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddPayment: () -> Unit,
    onToggleHistory: () -> Unit,
    onEditContribution: (ContributionEntity) -> Unit,
    onDeleteContribution: (ContributionEntity) -> Unit,
) {
    val status = memberPayStatus(balance.outstandingPaise, balance.paidPaise)
    val memberProgress = if (balance.expectedPaise > 0) {
        (balance.paidPaise.toFloat() / balance.expectedPaise.toFloat()).coerceAtLeast(0f)
    } else {
        0f
    }
    val balanceText = if (balance.hasExcess) {
        "Excess ${Money.formatInr(balance.excessPaise)}"
    } else {
        "Outstanding ${Money.formatInr(balance.outstandingPaise)}"
    }
    val balanceColor = when {
        balance.hasExcess -> CoastalTokens.accent
        balance.outstandingPaise > 0 -> CoastalTokens.danger
        else -> CoastalTokens.ink
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(CoastalTokens.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MemberAvatar(balance.name)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        balance.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = CoastalTokens.ink,
                    )
                    MemberStatusBadge(status)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Expected ${Money.formatInr(balance.expectedPaise)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoastalTokens.muted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Paid ${Money.formatInr(balance.paidPaise)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoastalTokens.muted,
                )
            }
        }

        Text(
            text = balanceText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = balanceColor,
        )

        CoastalProgressTrack(
            progress = memberProgress,
            fillColor = CoastalTokens.progressFill,
            height = 7.dp,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MemberActionChip(
                label = "Pay",
                icon = Icons.Outlined.Payments,
                onClick = onAddPayment,
                filled = true,
                modifier = Modifier.weight(1f),
            )
            MemberActionChip(
                label = "Edit",
                icon = Icons.Outlined.Edit,
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            )
            MemberActionChip(
                label = "Delete",
                icon = Icons.Outlined.Delete,
                onClick = onDelete,
                danger = true,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleHistory)
                .background(CoastalTokens.softFill)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (historyExpanded) "Hide payment history" else "Payment history",
                style = MaterialTheme.typography.labelLarge,
                color = CoastalTokens.tealSoftFg,
            )
            Icon(
                imageVector = if (historyExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = CoastalTokens.accent,
                modifier = Modifier.size(20.dp),
            )
        }

        if (historyExpanded) {
            if (history.isEmpty()) {
                Text(
                    "No payments recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoastalTokens.muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.forEach { contribution ->
                        ContributionHistoryRow(
                            contribution = contribution,
                            onEdit = { onEditContribution(contribution) },
                            onDelete = { onDeleteContribution(contribution) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    danger: Boolean = false,
) {
    val bg = when {
        filled -> Teal600
        danger -> CoastalTokens.dangerSoft
        else -> CoastalTokens.softFill
    }
    val fg = when {
        filled -> Color.White
        danger -> CoastalTokens.danger
        else -> CoastalTokens.ink
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (!filled && !danger) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
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
            .clip(RoundedCornerShape(12.dp))
            .background(CoastalTokens.softFillAlt)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                Money.formatInr(contribution.amountPaise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatter.format(Instant.ofEpochMilli(contribution.paidAt)),
                style = MaterialTheme.typography.bodySmall,
                color = CoastalTokens.muted,
            )
            contribution.note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = CoastalTokens.muted)
            }
        }
        TextButton(onClick = onEdit) { Text("Edit") }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}
