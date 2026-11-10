package com.tripbudget.app.ui.screens.expenses

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.PlannedExpenseEntity
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.ui.components.CategoryIcon
import com.tripbudget.app.ui.components.ConfirmDialog
import com.tripbudget.app.ui.components.EmptyState
import com.tripbudget.app.ui.components.ListShimmer
import com.tripbudget.app.ui.components.LivePullRefresh
import com.tripbudget.app.ui.components.SoftScreenCanvas
import com.tripbudget.app.ui.theme.CoastalTokens
import com.tripbudget.app.ui.theme.LoraFontFamily
import com.tripbudget.app.ui.theme.Teal600
import com.tripbudget.app.ui.theme.floatingNavContentPadding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class ExpensesSubTab { ACTUAL, PLAN }
private enum class ExpenseFilter { ALL, SHARED, PERSONAL }

@Composable
fun ExpensesScreen(
    tripId: Long,
    viewModel: ExpensesViewModel,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onPullProgress: (fraction: Float, tracking: Boolean) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentSubTab by rememberSaveable { mutableStateOf(ExpensesSubTab.ACTUAL) }

    // Actual expenses state
    var showAddActual by remember { mutableStateOf(false) }
    var editingActual by remember { mutableStateOf<ExpenseEntity?>(null) }
    var deletingActual by remember { mutableStateOf<ExpenseEntity?>(null) }
    var actualQuery by remember { mutableStateOf("") }
    var actualFilter by remember { mutableStateOf(ExpenseFilter.ALL) }

    // Planned expenses state
    var showAddPlanned by remember { mutableStateOf(false) }
    var editingPlanned by remember { mutableStateOf<PlannedExpenseEntity?>(null) }
    var deletingPlanned by remember { mutableStateOf<PlannedExpenseEntity?>(null) }
    var plannedQuery by remember { mutableStateOf("") }
    var plannedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    // Scroll-aware FAB visibility so the button never blocks content during scroll
    var isFabVisible by rememberSaveable { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) {
                    // Scrolling down: hide FAB to give full view of card details and actions
                    isFabVisible = false
                } else if (available.y > 10f) {
                    // Scrolling up: show FAB
                    isFabVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val dataReady = !state.isLoading && state.tripId == tripId
    if (!dataReady) {
        ListShimmer()
        return
    }

    val totalActualSpentPaise = remember(state.expenses) {
        state.expenses.sumOf { it.amountPaise }
    }

    val totalPlannedPaise = remember(state.plannedExpenses) {
        state.plannedExpenses.sumOf { it.amountPaise }
    }

    // Filter actual expenses
    val filteredActual = remember(state.expenses, actualQuery, actualFilter) {
        state.expenses
            .asSequence()
            .filter { expense ->
                when (actualFilter) {
                    ExpenseFilter.ALL -> true
                    ExpenseFilter.SHARED -> expense.paymentSource == PaymentSource.SHARED
                    ExpenseFilter.PERSONAL -> expense.paymentSource == PaymentSource.PERSONAL
                }
            }
            .filter { expense ->
                if (actualQuery.isBlank()) true
                else {
                    val q = actualQuery.trim().lowercase()
                    expense.title.lowercase().contains(q) ||
                        expense.category.label.lowercase().contains(q) ||
                        (expense.notes?.lowercase()?.contains(q) == true)
                }
            }
            .toList()
    }

    val groupedActual = remember(filteredActual) {
        filteredActual
            .groupBy { it.dateEpochDay }
            .toList()
            .sortedByDescending { it.first }
    }

    // Filter planned expenses
    val filteredPlanned = remember(state.plannedExpenses, plannedQuery, plannedCategoryFilter) {
        state.plannedExpenses
            .asSequence()
            .filter { item ->
                if (plannedCategoryFilter == null) true
                else item.category == plannedCategoryFilter
            }
            .filter { item ->
                if (plannedQuery.isBlank()) true
                else {
                    val q = plannedQuery.trim().lowercase()
                    item.title.lowercase().contains(q) ||
                        item.category.label.lowercase().contains(q) ||
                        (item.notes?.lowercase()?.contains(q) == true)
                }
            }
            .toList()
    }

    // Category-wise totals for comparison
    val categoryStats = remember(state.expenses, state.plannedExpenses) {
        ExpenseCategory.entries.mapNotNull { cat ->
            val planned = state.plannedExpenses.filter { it.category == cat }.sumOf { it.amountPaise }
            val spent = state.expenses.filter { it.category == cat }.sumOf { it.amountPaise }
            if (planned > 0L || spent > 0L) {
                CategoryPlanStat(category = cat, plannedPaise = planned, spentPaise = spent)
            } else null
        }
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    // Bottom clearance calculations:
    // SlidingBottomBar is ~84dp tall. FAB sits at floatingNavContentPadding() + 72dp.
    // List bottom padding clears both FAB and bottom bar with extra breathing room.
    val fabBottomPad = floatingNavContentPadding() + 72.dp
    val listBottomPad = floatingNavContentPadding() + 110.dp

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        if (currentSubTab == ExpensesSubTab.ACTUAL) {
                            showAddActual = true
                        } else {
                            showAddPlanned = true
                        }
                    },
                    containerColor = Teal600,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(bottom = fabBottomPad),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (currentSubTab == ExpensesSubTab.ACTUAL) "Add expense" else "Add planned item",
                    )
                }
            }
        },
    ) { padding ->
        LivePullRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onPullProgress = onPullProgress,
            modifier = Modifier.padding(padding),
        ) {
            SoftScreenCanvas {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 18.dp,
                        bottom = listBottomPad,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Top Sub-Tab Segmented Selector
                    item(key = "sub-tab-selector") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CoastalTokens.softFill)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val isActual = currentSubTab == ExpensesSubTab.ACTUAL
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(
                                        elevation = if (isActual) 2.dp else 0.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        clip = false,
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isActual) CoastalTokens.card else Color.Transparent)
                                    .clickable {
                                        currentSubTab = ExpensesSubTab.ACTUAL
                                        isFabVisible = true
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Actual Expenses",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isActual) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isActual) CoastalTokens.ink else CoastalTokens.muted,
                                )
                            }

                            val isPlan = currentSubTab == ExpensesSubTab.PLAN
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(
                                        elevation = if (isPlan) 2.dp else 0.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        clip = false,
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPlan) CoastalTokens.card else Color.Transparent)
                                    .clickable {
                                        currentSubTab = ExpensesSubTab.PLAN
                                        isFabVisible = true
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "Budget Plan",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isPlan) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isPlan) CoastalTokens.ink else CoastalTokens.muted,
                                    )
                                    if (state.plannedExpenses.isNotEmpty()) {
                                        Text(
                                            "${state.plannedExpenses.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPlan) Teal600 else CoastalTokens.muted,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(CoastalTokens.softFillAlt)
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // ---------------------------------------------------------
                    // ACTUAL EXPENSES SUB-TAB
                    // ---------------------------------------------------------
                    if (currentSubTab == ExpensesSubTab.ACTUAL) {
                        item(key = "actual-header") {
                            Text("Expenses", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Track shared and personal spend",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                            )
                            OutlinedTextField(
                                value = actualQuery,
                                onValueChange = { actualQuery = it },
                                placeholder = { Text("Search expenses") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = CoastalTokens.tealSoftFg,
                                    )
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
                                    val selected = actualFilter == option
                                    FilterChip(
                                        selected = selected,
                                        onClick = { actualFilter = option },
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

                        if (state.expenses.isEmpty()) {
                            item(key = "actual-empty") {
                                EmptyState(
                                    title = "No expenses yet",
                                    message = "Record food, fuel, stays, and other trip costs.",
                                    actionLabel = "Add expense",
                                    onAction = { showAddActual = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                )
                            }
                        } else if (groupedActual.isEmpty()) {
                            item(key = "actual-no-match") {
                                Text(
                                    "No expenses match your search.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 24.dp),
                                )
                            }
                        } else {
                            groupedActual.forEach { (epochDay, dayExpenses) ->
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
                                items(dayExpenses, key = { "expense-${it.id}" }) { expense ->
                                    val payer = expense.paidByMemberId?.let { id ->
                                        state.members.firstOrNull { it.id == id }?.name
                                    }
                                    ExpenseShowcaseRow(
                                        expense = expense,
                                        payerName = payer,
                                        onClick = { editingActual = expense },
                                        onEdit = { editingActual = expense },
                                        onDelete = { deletingActual = expense },
                                    )
                                }
                            }

                            // Inline action and extra breathing space after the last actual expense card
                            item(key = "actual-bottom-spacing") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(CoastalTokens.softFill)
                                            .clickable { showAddActual = true }
                                            .padding(vertical = 14.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = CoastalTokens.tealSoftFg,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.size(8.dp))
                                        Text(
                                            "Add another expense",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CoastalTokens.tealSoftFg,
                                        )
                                    }

                                    Text(
                                        text = "• End of expenses •",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoastalTokens.muted.copy(alpha = 0.6f),
                                    )

                                    Spacer(Modifier.height(36.dp))
                                }
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // BUDGET PLAN SUB-TAB
                    // ---------------------------------------------------------
                    if (currentSubTab == ExpensesSubTab.PLAN) {
                        item(key = "plan-hero") {
                            PlanSummaryHeroCard(
                                totalPlannedPaise = totalPlannedPaise,
                                totalSpentPaise = totalActualSpentPaise,
                                tripBudgetPaise = state.tripPlannedBudgetPaise,
                                onSyncBudget = {
                                    if (totalPlannedPaise > 0L) {
                                        viewModel.syncTripBudgetWithPlannedTotal(tripId, totalPlannedPaise)
                                    }
                                },
                            )
                        }

                        // Category Comparison Strip (if there is data)
                        if (categoryStats.isNotEmpty()) {
                            item(key = "plan-category-strip") {
                                Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                                    Text(
                                        "Category Breakdown",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CoastalTokens.ink,
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    ) {
                                        categoryStats.forEach { stat ->
                                            CategoryComparisonCard(stat = stat)
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "plan-controls") {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Planned Items",
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = LoraFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = CoastalTokens.ink,
                            )
                            Text(
                                "What the budget is reserved for (different from real spending)",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoastalTokens.muted,
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                            )
                            OutlinedTextField(
                                value = plannedQuery,
                                onValueChange = { plannedQuery = it },
                                placeholder = { Text("Search planned items") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = CoastalTokens.tealSoftFg,
                                    )
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
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                            ) {
                                val isAll = plannedCategoryFilter == null
                                FilterChip(
                                    selected = isAll,
                                    onClick = { plannedCategoryFilter = null },
                                    label = { Text("All Categories") },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CoastalTokens.tealSoft,
                                        selectedLabelColor = CoastalTokens.tealSoftFg,
                                        containerColor = CoastalTokens.card,
                                    ),
                                )
                                ExpenseCategory.entries.forEach { cat ->
                                    val isSelected = plannedCategoryFilter == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { plannedCategoryFilter = if (isSelected) null else cat },
                                        label = { Text(cat.label) },
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

                        if (state.plannedExpenses.isEmpty()) {
                            item(key = "plan-empty") {
                                EmptyState(
                                    title = "No budget planned yet",
                                    message = "Plan out expected costs for hotels, fuel, food, or activities before spending.",
                                    actionLabel = "Add planned item",
                                    onAction = { showAddPlanned = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                )
                            }
                        } else if (filteredPlanned.isEmpty()) {
                            item(key = "plan-no-match") {
                                Text(
                                    "No planned items match your search.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 24.dp),
                                )
                            }
                        } else {
                            items(filteredPlanned, key = { "planned-${it.id}" }) { item ->
                                val categorySpent = state.expenses
                                    .filter { it.category == item.category }
                                    .sumOf { it.amountPaise }
                                PlannedExpenseShowcaseRow(
                                    item = item,
                                    categorySpentPaise = categorySpent,
                                    onClick = { editingPlanned = item },
                                    onEdit = { editingPlanned = item },
                                    onDelete = { deletingPlanned = item },
                                )
                            }

                            // Extra space and inline action after the last planned item
                            item(key = "plan-bottom-spacing") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(CoastalTokens.softFill)
                                            .clickable { showAddPlanned = true }
                                            .padding(vertical = 14.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = CoastalTokens.tealSoftFg,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.size(8.dp))
                                        Text(
                                            "Add another planned item",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CoastalTokens.tealSoftFg,
                                        )
                                    }

                                    Text(
                                        text = "• End of budget plan •",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoastalTokens.muted.copy(alpha = 0.6f),
                                    )

                                    Spacer(Modifier.height(36.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Actual expense dialogs
    if (showAddActual) {
        ExpenseEditorDialog(
            members = state.members,
            existing = null,
            onDismiss = { showAddActual = false },
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

    editingActual?.let { expense ->
        ExpenseEditorDialog(
            members = state.members,
            existing = expense,
            onDismiss = { editingActual = null },
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

    deletingActual?.let { expense ->
        ConfirmDialog(
            title = "Delete expense?",
            message = "Remove \"${expense.title}\" (${Money.formatInr(expense.amountPaise)})?",
            onConfirm = {
                viewModel.deleteExpense(expense)
                deletingActual = null
            },
            onDismiss = { deletingActual = null },
        )
    }

    // Planned expense dialogs
    if (showAddPlanned) {
        PlannedExpenseEditorDialog(
            existing = null,
            onDismiss = { showAddPlanned = false },
            onSave = { title, amount, category, notes, onResult ->
                viewModel.savePlannedExpense(
                    existing = null,
                    tripId = tripId,
                    title = title,
                    amountRupees = amount,
                    category = category,
                    notes = notes,
                    onResult = onResult,
                )
            },
        )
    }

    editingPlanned?.let { item ->
        PlannedExpenseEditorDialog(
            existing = item,
            onDismiss = { editingPlanned = null },
            onSave = { title, amount, category, notes, onResult ->
                viewModel.savePlannedExpense(
                    existing = item,
                    tripId = tripId,
                    title = title,
                    amountRupees = amount,
                    category = category,
                    notes = notes,
                    onResult = onResult,
                )
            },
        )
    }

    deletingPlanned?.let { item ->
        ConfirmDialog(
            title = "Delete planned item?",
            message = "Remove planned \"${item.title}\" (${Money.formatInr(item.amountPaise)})?",
            onConfirm = {
                viewModel.deletePlannedExpense(item)
                deletingPlanned = null
            },
            onDismiss = { deletingPlanned = null },
        )
    }
}

private data class CategoryPlanStat(
    val category: ExpenseCategory,
    val plannedPaise: Long,
    val spentPaise: Long,
)

@Composable
private fun PlanSummaryHeroCard(
    totalPlannedPaise: Long,
    totalSpentPaise: Long,
    tripBudgetPaise: Long,
    onSyncBudget: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(CoastalTokens.card)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    "TOTAL PLANNED BUDGET",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CoastalTokens.muted,
                )
                Text(
                    Money.formatInr(totalPlannedPaise),
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = CoastalTokens.ink,
                )
            }

            if (tripBudgetPaise > 0L) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Trip Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = CoastalTokens.muted,
                    )
                    Text(
                        Money.formatInr(tripBudgetPaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CoastalTokens.ink,
                    )
                }
            } else if (totalPlannedPaise > 0L) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CoastalTokens.tealSoft)
                        .clickable(onClick = onSyncBudget)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Sync,
                        contentDescription = null,
                        tint = CoastalTokens.tealSoftFg,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "Set as Target",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CoastalTokens.tealSoftFg,
                    )
                }
            }
        }

        // Budget allocation progress (if trip budget target is set)
        if (tripBudgetPaise > 0L) {
            val allocatedRatio = (totalPlannedPaise.toFloat() / tripBudgetPaise.toFloat()).coerceIn(0f, 1f)
            val diffPaise = tripBudgetPaise - totalPlannedPaise
            val isOver = diffPaise < 0

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        if (isOver) "Budget exceeded by ${Money.formatInr(-diffPaise)}"
                        else "${Money.formatInr(diffPaise)} buffer remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOver) CoastalTokens.accent else CoastalTokens.tealSoftFg,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${(allocatedRatio * 100).toInt()}% planned",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoastalTokens.muted,
                    )
                }
                LinearProgressIndicator(
                    progress = { allocatedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (isOver) CoastalTokens.accent else Teal600,
                    trackColor = CoastalTokens.softFill,
                    strokeCap = StrokeCap.Round,
                )
            }
        }

        // Spend vs Planned comparison strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CoastalTokens.softFill)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = CoastalTokens.muted,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Spent so far:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoastalTokens.muted,
                )
            }
            Text(
                "${Money.formatInr(totalSpentPaise)} / ${Money.formatInr(totalPlannedPaise)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (totalSpentPaise > totalPlannedPaise && totalPlannedPaise > 0L) CoastalTokens.accent else CoastalTokens.ink,
            )
        }
    }
}

@Composable
private fun CategoryComparisonCard(stat: CategoryPlanStat) {
    val isOver = stat.spentPaise > stat.plannedPaise && stat.plannedPaise > 0L
    Column(
        modifier = Modifier
            .shadow(2.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(CoastalTokens.card)
            .padding(12.dp)
            .size(width = 145.dp, height = 115.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CategoryIcon(stat.category)
            Text(
                stat.category.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = CoastalTokens.ink,
                maxLines = 1,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Plan: ${Money.formatInr(stat.plannedPaise)}",
                style = MaterialTheme.typography.bodySmall,
                color = CoastalTokens.muted,
            )
            Text(
                "Spent: ${Money.formatInr(stat.spentPaise)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isOver) CoastalTokens.accent else CoastalTokens.tealSoftFg,
            )
        }
    }
}

@Composable
private fun PlannedExpenseShowcaseRow(
    item: PlannedExpenseEntity,
    categorySpentPaise: Long,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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
        CategoryIcon(item.category)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (!item.notes.isNullOrBlank()) "${item.category.label} · ${item.notes}"
                else item.category.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
            )
            if (categorySpentPaise > 0L) {
                Text(
                    "Spent in ${item.category.label}: ${Money.formatInr(categorySpentPaise)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoastalTokens.tealSoftFg,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Money.formatInr(item.amountPaise),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = CoastalTokens.ink,
            )
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit planned item",
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete planned item",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
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
