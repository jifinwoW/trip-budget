package com.tripbudget.app.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tripbudget.app.R
import com.tripbudget.app.ui.theme.LoraFontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tripbudget.app.data.ThemePreferences
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.domain.TripDates
import com.tripbudget.app.ui.TripSelectionUiState
import com.tripbudget.app.ui.TripSelectionViewModel
import com.tripbudget.app.ui.components.ConfirmDialog
import com.tripbudget.app.ui.components.EmptyState
import com.tripbudget.app.ui.components.LoadingContent
import com.tripbudget.app.ui.screens.dashboard.DashboardScreen
import com.tripbudget.app.ui.screens.dashboard.DashboardViewModel
import com.tripbudget.app.ui.screens.expenses.ExpensesScreen
import com.tripbudget.app.ui.screens.expenses.ExpensesViewModel
import com.tripbudget.app.ui.screens.members.MembersScreen
import com.tripbudget.app.ui.screens.members.MembersViewModel
import com.tripbudget.app.ui.screens.settings.SettingsScreen
import com.tripbudget.app.ui.screens.settings.SettingsViewModel
import com.tripbudget.app.ui.screens.trips.TripEditorDialog
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripBudgetNavHost(
    repository: TripRepository,
    themePreferences: ThemePreferences,
) {
    val tripSelectionVm: TripSelectionViewModel = viewModel(
        factory = TripSelectionViewModel.factory(repository),
    )
    val tripIdFlow = remember(tripSelectionVm) {
        tripSelectionVm.uiState.map { it.selectedTripId }
    }
    // Always create ViewModels unconditionally (Compose rule); they stay idle until a trip exists.
    val dashboardVm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(repository, tripIdFlow),
    )
    val expensesVm: ExpensesViewModel = viewModel(
        factory = ExpensesViewModel.factory(repository, tripIdFlow),
    )
    val membersVm: MembersViewModel = viewModel(
        factory = MembersViewModel.factory(repository, tripIdFlow),
    )
    val settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(themePreferences),
    )

    val tripState by tripSelectionVm.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    var showCreateTrip by remember { mutableStateOf(false) }
    var showEditTrip by remember { mutableStateOf(false) }
    var showDeleteTrip by remember { mutableStateOf(false) }
    var tripMenuExpanded by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    when {
        tripState.isLoading -> {
            LoadingContent()
        }
        tripState.trips.isEmpty() -> {
            EmptyState(
                title = stringResource(R.string.brand_name),
                message = "Create your first trip to track budget, contributions, and expenses offline.",
                actionLabel = "Create trip",
                onAction = { showCreateTrip = true },
            )
            if (showCreateTrip) {
                TripEditorDialog(
                    existing = null,
                    onDismiss = { showCreateTrip = false },
                    onSave = { name, destination, budget, startDate, endDate, onResult ->
                        tripSelectionVm.createTrip(
                            name,
                            destination,
                            budget,
                            startDate,
                            endDate,
                            onResult,
                        )
                    },
                )
            }
        }
        else -> {
            val selectedTrip = tripState.selectedTrip
            if (selectedTrip == null) {
                LoadingContent()
            } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    CoastalAppHeader(
                        state = tripState,
                        tripMenuExpanded = tripMenuExpanded,
                        moreMenuExpanded = moreMenuExpanded,
                        onTripMenuExpandedChange = { tripMenuExpanded = it },
                        onMoreMenuExpandedChange = { moreMenuExpanded = it },
                        onSelectTrip = {
                            tripSelectionVm.selectTrip(it)
                            tripMenuExpanded = false
                        },
                        onCreateTrip = {
                            tripMenuExpanded = false
                            moreMenuExpanded = false
                            showCreateTrip = true
                        },
                        onEditTrip = {
                            moreMenuExpanded = false
                            showEditTrip = true
                        },
                        onDeleteTrip = {
                            moreMenuExpanded = false
                            showDeleteTrip = true
                        },
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        AppDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = {
                                    Text(
                                        dest.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.DASHBOARD.route,
                    modifier = Modifier.padding(padding),
                ) {
                    composable(AppDestination.DASHBOARD.route) {
                        DashboardScreen(
                            trip = selectedTrip,
                            viewModel = dashboardVm,
                            onEditTrip = { showEditTrip = true },
                            onViewAllExpenses = {
                                navController.navigate(AppDestination.EXPENSES.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                    composable(AppDestination.EXPENSES.route) {
                        ExpensesScreen(tripId = selectedTrip.id, viewModel = expensesVm)
                    }
                    composable(AppDestination.MEMBERS.route) {
                        MembersScreen(tripId = selectedTrip.id, viewModel = membersVm)
                    }
                    composable(AppDestination.SETTINGS.route) {
                        SettingsScreen(settingsVm)
                    }
                }
            }

            if (showCreateTrip) {
                TripEditorDialog(
                    existing = null,
                    onDismiss = { showCreateTrip = false },
                    onSave = { name, destination, budget, startDate, endDate, onResult ->
                        tripSelectionVm.createTrip(
                            name,
                            destination,
                            budget,
                            startDate,
                            endDate,
                            onResult,
                        )
                    },
                )
            }
            if (showEditTrip) {
                TripEditorDialog(
                    existing = selectedTrip,
                    onDismiss = { showEditTrip = false },
                    onSave = { name, destination, budget, startDate, endDate, onResult ->
                        tripSelectionVm.updateTrip(
                            selectedTrip,
                            name,
                            destination,
                            budget,
                            startDate,
                            endDate,
                            onResult,
                        )
                    },
                )
            }
            if (showDeleteTrip) {
                ConfirmDialog(
                    title = "Delete trip?",
                    message = "Delete \"${selectedTrip.name}\" and all members, contributions, and expenses?",
                    onConfirm = {
                        tripSelectionVm.deleteTrip(selectedTrip) {
                            showDeleteTrip = false
                        }
                    },
                    onDismiss = { showDeleteTrip = false },
                )
            }
            }
        }
    }
}

/**
 * Showcase top bar (design image):
 * soft sky-blue/white cloud wash behind
 * TripBudget brand | white pill trip switcher | teal + | ⋮ more
 */
@Composable
private fun CoastalAppHeader(
    state: TripSelectionUiState,
    tripMenuExpanded: Boolean,
    moreMenuExpanded: Boolean,
    onTripMenuExpandedChange: (Boolean) -> Unit,
    onMoreMenuExpandedChange: (Boolean) -> Unit,
    onSelectTrip: (Long) -> Unit,
    onCreateTrip: () -> Unit,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD4EBF5),
                        Color(0xFFE8F4FA),
                        Color(0xFFF5FBFC),
                    ),
                ),
            ),
    ) {
        // Soft white cloud patches mixed into the sky wash.
        Canvas(modifier = Modifier.matchParentSize()) {
            fun cloud(cx: Float, cy: Float, radius: Float, alpha: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alpha),
                            Color.White.copy(alpha = 0f),
                        ),
                        center = Offset(cx, cy),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(cx, cy),
                )
            }
            cloud(size.width * 0.12f, size.height * 0.55f, size.minDimension * 0.55f, 0.7f)
            cloud(size.width * 0.38f, size.height * 0.35f, size.minDimension * 0.42f, 0.55f)
            cloud(size.width * 0.68f, size.height * 0.6f, size.minDimension * 0.5f, 0.6f)
            cloud(size.width * 0.92f, size.height * 0.4f, size.minDimension * 0.38f, 0.5f)
            cloud(size.width * 0.55f, size.height * 0.85f, size.minDimension * 0.35f, 0.4f)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.brand_name),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2A26),
                maxLines = 1,
            )

            Spacer(Modifier.weight(1f))

            Box {
                Row(
                    modifier = Modifier
                        .widthIn(max = 168.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, Color(0xFFD7E4E1), RoundedCornerShape(999.dp))
                        .background(Color.White)
                        .clickable { onTripMenuExpandedChange(true) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = state.selectedTrip?.name ?: "Select trip",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A2B28),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Switch trip",
                        tint = Color(0xFF0D8B8A),
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = tripMenuExpanded,
                    onDismissRequest = { onTripMenuExpandedChange(false) },
                ) {
                    state.trips.forEach { trip ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = buildString {
                                        append(trip.name)
                                        trip.destination?.let { append(" · $it") }
                                        TripDates.formatRange(trip)?.let { append(" · $it") }
                                    },
                                )
                            },
                            onClick = { onSelectTrip(trip.id) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Create new trip") },
                        onClick = onCreateTrip,
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D8B8A))
                    .clickable(onClick = onCreateTrip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New trip",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Box {
                IconButton(
                    onClick = { onMoreMenuExpandedChange(true) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color(0xFF5A6F6A),
                    )
                }
                DropdownMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { onMoreMenuExpandedChange(false) },
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit trip") },
                        onClick = onEditTrip,
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete trip") },
                        onClick = onDeleteTrip,
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    )
                }
            }
        }
    }
}
