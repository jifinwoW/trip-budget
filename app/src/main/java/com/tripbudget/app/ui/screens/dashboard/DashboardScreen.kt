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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.R
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.TripDates
import com.tripbudget.app.places.WeatherKind
import com.tripbudget.app.ui.components.CategoryIcon
import com.tripbudget.app.ui.components.EnterCascade
import com.tripbudget.app.ui.components.ExpenseSourceChip
import com.tripbudget.app.ui.components.LivePullRefresh
import com.tripbudget.app.ui.components.DashboardShimmer
import com.tripbudget.app.ui.components.PremiumMetricTile
import com.tripbudget.app.ui.components.PrimaryCoastalButton
import com.tripbudget.app.ui.components.RoadMapRouteChip
import com.tripbudget.app.ui.components.SecondaryCoastalButton
import com.tripbudget.app.ui.components.SharedCashHeroCard
import com.tripbudget.app.ui.components.SoftCategoryChip
import com.tripbudget.app.ui.components.SoftProgressCard
import com.tripbudget.app.ui.screens.expenses.ExpenseEditorDialog
import com.tripbudget.app.ui.screens.members.ContributionEditorDialog
import com.tripbudget.app.ui.theme.CoastalTokens
import com.tripbudget.app.ui.theme.Teal600
import com.tripbudget.app.ui.theme.floatingNavContentPadding
import java.io.File
import java.time.LocalDate

@Composable
fun DashboardScreen(
    trip: TripEntity,
    viewModel: DashboardViewModel,
    headerWeatherKind: WeatherKind? = null,
    weatherUnderHero: Dp = 36.dp,
    heroCorner: Dp = 24.dp,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onPullProgress: (Float, Boolean) -> Unit = { _, _ -> },
    onEditTrip: () -> Unit = {},
    onViewAllExpenses: () -> Unit = {},
    onOpenRoutePlan: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddContribution by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }

    val totals = state.totals
    val totalsReady = !state.isLoading && state.trip?.id == trip.id && totals != null

    if (!totalsReady || totals == null) {
        DashboardShimmer()
        return
    }

    val sharedColor = if (totals.sharedCashRemainingPaise < 0) {
        if (CoastalTokens.isDark) Color(0xFFFFB4A8) else Color(0xFFFFD4CE)
    } else {
        Color.White
    }
    val budgetAmountColor = if (totals.isOverBudget) {
        CoastalTokens.danger
    } else {
        CoastalTokens.ink
    }
    val mintTile = CoastalTokens.tileMint()
    val roseTile = CoastalTokens.tileRose()
    val blueTile = CoastalTokens.tileBlue()
    val sandTile = CoastalTokens.tileSand()
    val peachTile = CoastalTokens.tilePeach()
    val warmTile = CoastalTokens.tileWarm()
    val sheetColors = if (CoastalTokens.isDark) {
        listOf(Color(0xFF12201E), Color(0xFF0E1A18), Color(0xFF0C1816))
    } else {
        listOf(Color(0xFFF7FCFD), Color(0xFFEAF4F8), Color(0xFFF5FBFC))
    }
    val mistStrength = if (CoastalTokens.isDark) 0.18f else 1f

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

    // Keep rounded hero corners; rain sky from NavHost shows through the crescents
    // because the top of this screen stays transparent for [weatherUnderHero].
    val heroHeight = 210.dp
    val heroOverlap = 24.dp
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val chipStayInteractivePx = with(density) { 40.dp.roundToPx() }
    val zoomRangePx = with(density) { 240.dp.roundToPx() }.coerceAtLeast(1)
    // Chip stays fixed on the hero; sits above the scroll layer only while still visible.
    val budgetChipOnTop = scrollState.value < chipStayInteractivePx
    // Subtle Ken Burns zoom + drift as content scrolls over the hero.
    val heroImageZoom by remember(zoomRangePx) {
        derivedStateOf {
            val progress = (scrollState.value.toFloat() / zoomRangePx).coerceIn(0f, 1f)
            1f + progress * 0.50f
        }
    }
    val heroImageOffsetY by remember {
        derivedStateOf {
            // Ease the photo upward a little so zoom feels anchored, not clipped oddly.
            (-scrollState.value * 0.18f).coerceIn(-90f, 0f)
        }
    }
    LivePullRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        onPullProgress = onPullProgress,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Canvas starts below the corner zone so continuous rain isn’t covered.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = weatherUnderHero.coerceAtMost(heroCorner + 8.dp))
                    .background(CoastalTokens.canvas),
            )
            TripScenicHeader(
                tripName = trip.name,
                dateRange = TripDates.formatRange(trip),
                plannedBudgetPaise = totals.plannedBudgetPaise,
                coverImagePath = trip.coverImagePath,
                onEditBudget = onEditTrip,
                showBudgetChip = false,
                imageZoom = heroImageZoom,
                imageOffsetY = heroImageOffsetY,
                height = heroHeight,
                topCornerRadius = heroCorner,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .verticalScroll(scrollState),
            ) {
            // Same visual tuck as before: sheet starts 24dp up into the hero.
            Spacer(Modifier.height(heroHeight - heroOverlap))

            // Soft sky-blue + white mist sheet behind Shared Cash / tiles / buttons.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Brush.verticalGradient(colors = sheetColors)),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    fun mist(cx: Float, cy: Float, radius: Float, color: Color, alpha: Float) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    color.copy(alpha = alpha * mistStrength),
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
                        .padding(top = 20.dp, bottom = 12.dp),
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
                                .background(CoastalTokens.dangerSoft)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.TrendingDown, null, tint = CoastalTokens.danger)
                            Column {
                                Text(
                                    "Over budget",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CoastalTokens.danger,
                                )
                                Text(
                                    Money.formatInr(-totals.budgetRemainingPaise) + " over plan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (CoastalTokens.isDark) Color(0xFFFFB4A8) else Color(0xFF5C180C),
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
                                tileColor = mintTile.background,
                                iconBg = mintTile.iconBg,
                                iconTint = mintTile.iconTint,
                                amountColor = mintTile.amount,
                                modifier = Modifier.weight(1f),
                            )
                            PremiumMetricTile(
                                label = "Shared spent",
                                amountPaise = totals.sharedExpensesPaise,
                                icon = Icons.Outlined.ShoppingBag,
                                tileColor = roseTile.background,
                                iconBg = roseTile.iconBg,
                                iconTint = roseTile.iconTint,
                                amountColor = roseTile.amount,
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
                            tileColor = blueTile.background,
                            iconBg = blueTile.iconBg,
                            iconTint = blueTile.iconTint,
                            amountColor = budgetAmountColor,
                            modifier = Modifier.weight(1f),
                        )
                        PremiumMetricTile(
                            label = "Planned",
                            amountPaise = totals.plannedBudgetPaise,
                            icon = Icons.Outlined.Flag,
                            tileColor = sandTile.background,
                            iconBg = sandTile.iconBg,
                            iconTint = sandTile.iconTint,
                            amountColor = sandTile.amount,
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
                            tileColor = peachTile.background,
                            iconBg = peachTile.iconBg,
                            iconTint = peachTile.iconTint,
                            amountColor = peachTile.amount,
                            modifier = Modifier.weight(1f),
                        )
                        PremiumMetricTile(
                            label = "Outstanding",
                            amountPaise = totals.outstandingContributionsPaise,
                            icon = Icons.Outlined.PendingActions,
                            tileColor = warmTile.background,
                            iconBg = warmTile.iconBg,
                            iconTint = warmTile.iconTint,
                            amountColor = warmTile.amount,
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
                                    .background(CoastalTokens.softFill)
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

            // Clears the floating nav.
            Spacer(Modifier.height(floatingNavContentPadding()))
            }

            // Fixed on the hero (does not scroll with tiles); tappable while visible.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(heroHeight)
                    .zIndex(if (budgetChipOnTop) 2f else 0f),
            ) {
                PlannedBudgetEditChip(
                    plannedBudgetPaise = totals.plannedBudgetPaise,
                    onEditBudget = onEditTrip,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 36.dp),
                )
            }

            RoadMapRouteChip(
                stopCount = state.routeStopCount,
                nextStopTitle = state.nextStopTitle,
                onClick = onOpenRoutePlan,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = floatingNavContentPadding() + 36.dp)
                    .zIndex(3f),
            )
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
 * date row, planned-budget chip bottom-right.
 * Top corners rounded only.
 */
@Composable
private fun TripScenicHeader(
    tripName: String,
    dateRange: String?,
    plannedBudgetPaise: Long,
    coverImagePath: String?,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier,
    showBudgetChip: Boolean = true,
    imageZoom: Float = 1f,
    imageOffsetY: Float = 0f,
    height: Dp = 210.dp,
    topCornerRadius: Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(topStart = topCornerRadius, topEnd = topCornerRadius)),
    ) {
        val coverFile = coverImagePath?.let { File(it) }?.takeIf { it.exists() }
        val imageModifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                scaleX = imageZoom
                scaleY = imageZoom
                translationY = imageOffsetY
            }
        if (coverFile != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverFile)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.bg_trip_header),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
        }
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
            if (dateRange != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }
        }

        if (showBudgetChip) {
            PlannedBudgetEditChip(
                plannedBudgetPaise = plannedBudgetPaise,
                onEditBudget = onEditBudget,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 36.dp),
            )
        }
    }
}

@Composable
private fun PlannedBudgetEditChip(
    plannedBudgetPaise: Long,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(CoastalTokens.card)
            .clickable(onClick = onEditBudget)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            Text(
                "Planned Budget",
                style = MaterialTheme.typography.labelMedium,
                color = CoastalTokens.muted,
            )
            Text(
                Money.formatInr(plannedBudgetPaise),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CoastalTokens.ink,
            )
        }
        Icon(
            Icons.Outlined.Edit,
            contentDescription = "Edit trip",
            tint = CoastalTokens.muted,
            modifier = Modifier.size(16.dp),
        )
    }
}
