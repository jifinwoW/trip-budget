package com.tripbudget.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.R
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.components.CategoryIcon
import com.tripbudget.app.ui.components.EnterCascade
import com.tripbudget.app.ui.components.ExpenseSourceChip
import com.tripbudget.app.ui.components.LoadingContent
import com.tripbudget.app.ui.components.PremiumMetricTile
import com.tripbudget.app.ui.components.PrimaryCoastalButton
import com.tripbudget.app.ui.components.SecondaryCoastalButton
import com.tripbudget.app.ui.components.SharedCashHeroCard
import com.tripbudget.app.ui.components.SoftCategoryChip
import com.tripbudget.app.ui.components.SoftDashboardCanvas
import com.tripbudget.app.ui.components.SoftProgressCard
import com.tripbudget.app.ui.screens.expenses.ExpenseEditorDialog
import com.tripbudget.app.ui.screens.members.ContributionEditorDialog
import com.tripbudget.app.ui.theme.CoastalTokens
import com.tripbudget.app.ui.theme.Teal600
import java.time.LocalDate

@Composable
fun DashboardScreen(
    trip: TripEntity,
    viewModel: DashboardViewModel,
    onEditTrip: () -> Unit = {},
    onViewAllExpenses: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddContribution by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }

    val totals = state.totals
    val totalsReady = !state.isLoading && state.trip?.id == trip.id && totals != null

    if (!totalsReady || totals == null) {
        SoftDashboardCanvas {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = trip.destination?.takeIf { it.isNotBlank() } ?: "Trip",
                        style = MaterialTheme.typography.labelLarge,
                        color = Teal600,
                    )
                    Text(trip.name, style = MaterialTheme.typography.headlineLarge)
                }
                LoadingContent(modifier = Modifier.weight(1f))
            }
        }
        return
    }

    val sharedColor = if (totals.sharedCashRemainingPaise < 0) {
        Color(0xFFFFD4CE)
    } else {
        Color.White
    }
    val budgetAmountColor = if (totals.isOverBudget) {
        CoastalTokens.negative
    } else {
        Color(0xFF1A2B28)
    }

    // Progress logic (same as design):
    // Budget used % = total expenses / planned budget
    // Contributions collected % = collected payments / sum of member expected amounts
    val expectedTotal = totals.memberBalances.sumOf { it.expectedPaise }
    val budgetUsedRatio = if (totals.plannedBudgetPaise > 0L) {
        totals.totalExpensesPaise.toFloat() / totals.plannedBudgetPaise.toFloat()
    } else {
        0f
    }.coerceAtLeast(0f)
    val collectedRatio = if (expectedTotal > 0L) {
        totals.contributionsCollectedPaise.toFloat() / expectedTotal.toFloat()
    } else {
        0f
    }.coerceAtLeast(0f)

    // Showcase layout: photo hero → sky-mist sheet with rounded top overlapping it.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FBFC)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            EnterCascade(0) {
                TripScenicHeader(
                    tripName = trip.name,
                    destination = trip.destination,
                    plannedBudgetPaise = totals.plannedBudgetPaise,
                    onEditBudget = onEditTrip,
                )
            }

            // Soft sky-blue + white mist sheet behind Shared Cash / tiles / buttons.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF7FCFD),
                                Color(0xFFEAF4F8),
                                Color(0xFFF5FBFC),
                            ),
                        ),
                    ),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    fun mist(cx: Float, cy: Float, radius: Float, color: Color, alpha: Float) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color.copy(alpha = alpha),
                                    color.copy(alpha = 0f),
                                ),
                                center = Offset(cx, cy),
                                radius = radius,
                            ),
                            radius = radius,
                            center = Offset(cx, cy),
                        )
                    }
                    // Stronger mist on the left, like the design wash.
                    mist(size.width * 0.02f, size.height * 0.12f, size.minDimension * 0.7f, Color(0xFFBFE4F0), 0.45f)
                    mist(size.width * 0.08f, size.height * 0.28f, size.minDimension * 0.55f, Color(0xFFD2EEF5), 0.5f)
                    mist(size.width * 0.0f, size.height * 0.48f, size.minDimension * 0.65f, Color(0xFFC8E8F2), 0.35f)
                    mist(size.width * 0.18f, size.height * 0.72f, size.minDimension * 0.5f, Color.White, 0.55f)
                    mist(size.width * 0.85f, size.height * 0.2f, size.minDimension * 0.4f, Color.White, 0.35f)
                    mist(size.width * 0.7f, size.height * 0.55f, size.minDimension * 0.35f, Color(0xFFE8F6FA), 0.3f)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp, bottom = 28.dp),
                ) {
                EnterCascade(40) {
                    SoftProgressCard(
                        budgetUsed = budgetUsedRatio,
                        collectedProgress = collectedRatio,
                        overBudget = totals.isOverBudget,
                    )
                }

                if (totals.isOverBudget) {
                    Spacer(Modifier.height(12.dp))
                    EnterCascade(60) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFCE8E4))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.TrendingDown, null, tint = CoastalTokens.negative)
                            Column {
                                Text(
                                    "Over budget",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CoastalTokens.negative,
                                )
                                Text(
                                    Money.formatInr(-totals.budgetRemainingPaise) + " over plan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF5C180C),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                EnterCascade(80) {
                    // Shared Cash hero with first tile row overlapping its bottom edge.
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SharedCashHeroCard(
                            amountPaise = totals.sharedCashRemainingPaise,
                            subtitle = "Available in the group fund",
                            amountColor = sharedColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 62.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .zIndex(1f),
                        ) {
                            PremiumMetricTile(
                                label = "Collected",
                                amountPaise = totals.contributionsCollectedPaise,
                                icon = Icons.Outlined.Payments,
                                tileColor = Color(0xFFD8F3EE),
                                iconBg = Color(0xFFB5E6DC),
                                iconTint = Color(0xFF0D8B8A),
                                modifier = Modifier.weight(1f),
                            )
                            PremiumMetricTile(
                                label = "Shared spent",
                                amountPaise = totals.sharedExpensesPaise,
                                icon = Icons.Outlined.ShoppingBag,
                                tileColor = Color(0xFFF6DCE0),
                                iconBg = Color(0xFFF0C4CA),
                                iconTint = Color(0xFFC45B66),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                EnterCascade(120) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PremiumMetricTile(
                            label = "Budget left",
                            amountPaise = totals.budgetRemainingPaise,
                            icon = Icons.Outlined.Savings,
                            tileColor = Color(0xFFD7EAF7),
                            iconBg = Color(0xFFBDDCF0),
                            iconTint = Color(0xFF2F7DB8),
                            amountColor = budgetAmountColor,
                            modifier = Modifier.weight(1f),
                        )
                        PremiumMetricTile(
                            label = "Planned",
                            amountPaise = totals.plannedBudgetPaise,
                            icon = Icons.Outlined.Flag,
                            tileColor = Color(0xFFF3E8D4),
                            iconBg = Color(0xFFE6D4B0),
                            iconTint = Color(0xFFB07A2E),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PremiumMetricTile(
                            label = "Total spent",
                            amountPaise = totals.totalExpensesPaise,
                            icon = Icons.Outlined.CreditCard,
                            tileColor = Color(0xFFF8E0D0),
                            iconBg = Color(0xFFF0C8B4),
                            iconTint = Color(0xFFD0653A),
                            modifier = Modifier.weight(1f),
                        )
                        PremiumMetricTile(
                            label = "Outstanding",
                            amountPaise = totals.outstandingContributionsPaise,
                            icon = Icons.Outlined.PendingActions,
                            tileColor = Color(0xFFF6E4DC),
                            iconBg = Color(0xFFEED0C4),
                            iconTint = Color(0xFFC47A5A),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                EnterCascade(160) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PrimaryCoastalButton(
                            text = "Contribute",
                            onClick = { showAddContribution = true },
                            enabled = state.members.isNotEmpty(),
                            icon = Icons.Outlined.Payments,
                            modifier = Modifier.weight(1f).height(52.dp),
                        )
                        SecondaryCoastalButton(
                            text = "Expense",
                            onClick = { showAddExpense = true },
                            icon = Icons.Outlined.Add,
                            modifier = Modifier.weight(1f).height(52.dp),
                        )
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

                Spacer(Modifier.height(22.dp))
                EnterCascade(200) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Recent expenses", style = MaterialTheme.typography.titleLarge)
                        if (state.recentExpenses.isNotEmpty()) {
                            TextButton(onClick = onViewAllExpenses) {
                                Text("View all", color = Teal600)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.recentExpenses.isEmpty()) {
                        Text(
                            "No expenses recorded yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        state.recentExpenses.forEach { expense ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .shadow(3.dp, RoundedCornerShape(18.dp), clip = false)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFFF7FAFA))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CategoryIcon(expense.category)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(expense.title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SoftCategoryChip(text = expense.category.label)
                                        ExpenseSourceChip(source = expense.paymentSource)
                                    }
                                    Text(
                                        LocalDate.ofEpochDay(expense.dateEpochDay).toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                                Text(
                                    Money.formatInr(expense.amountPaise),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    if (showAddContribution) {
        ContributionEditorDialog(
            members = state.members,
            existing = null,
            defaultMemberId = state.members.firstOrNull()?.id,
            onDismiss = { showAddContribution = false },
            onSave = { memberId, amount, note, paidAt, onResult ->
                viewModel.addContribution(trip.id, memberId, amount, note, paidAt, onResult)
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

/**
 * Showcase hero: full-width landscape, title top-left,
 * destination row, planned-budget chip bottom-right.
 * Top corners rounded only.
 */
@Composable
private fun TripScenicHeader(
    tripName: String,
    destination: String?,
    plannedBudgetPaise: Long,
    onEditBudget: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_trip_header),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.38f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Text(
                text = tripName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = destination?.takeIf { it.isNotBlank() } ?: "Add a destination",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 36.dp)
                .shadow(4.dp, RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable(onClick = onEditBudget)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column {
                Text(
                    "Planned Budget",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF5A6F6A),
                )
                Text(
                    Money.formatInr(plannedBudgetPaise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A2B28),
                )
            }
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Edit trip",
                tint = Color(0xFF8A9E99),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
