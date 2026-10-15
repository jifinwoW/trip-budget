package com.tripbudget.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tripbudget.app.data.ThemePreferences
import com.tripbudget.app.data.TripRepository
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

    when {
        tripState.isLoading -> {
            LoadingContent()
        }
        tripState.trips.isEmpty() -> {
            EmptyState(
                title = "TripBudget",
                message = "Create your first trip to track budget, contributions, and expenses offline.",
                actionLabel = "Create trip",
                onAction = { showCreateTrip = true },
            )
            if (showCreateTrip) {
                TripEditorDialog(
                    existing = null,
                    onDismiss = { showCreateTrip = false },
                    onSave = { name, destination, budget, onResult ->
                        tripSelectionVm.createTrip(name, destination, budget, onResult)
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
                    TopAppBar(
                        title = {
                            TripSelector(
                                state = tripState,
                                expanded = tripMenuExpanded,
                                onExpandedChange = { tripMenuExpanded = it },
                                onSelect = {
                                    tripSelectionVm.selectTrip(it)
                                    tripMenuExpanded = false
                                },
                                onCreate = {
                                    tripMenuExpanded = false
                                    showCreateTrip = true
                                },
                            )
                        },
                        actions = {
                            IconButton(onClick = { showEditTrip = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit trip")
                            }
                            IconButton(onClick = { showDeleteTrip = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete trip")
                            }
                            IconButton(onClick = { showCreateTrip = true }) {
                                Icon(Icons.Default.Add, contentDescription = "New trip")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = NavigationBarDefaults.Elevation,
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
                                        dest.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
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
                        DashboardScreen(trip = selectedTrip, viewModel = dashboardVm)
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
                    onSave = { name, destination, budget, onResult ->
                        tripSelectionVm.createTrip(name, destination, budget, onResult)
                    },
                )
            }
            if (showEditTrip) {
                TripEditorDialog(
                    existing = selectedTrip,
                    onDismiss = { showEditTrip = false },
                    onSave = { name, destination, budget, onResult ->
                        tripSelectionVm.updateTrip(selectedTrip, name, destination, budget, onResult)
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

@Composable
private fun TripSelector(
    state: TripSelectionUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    androidx.compose.foundation.layout.Box {
        TextButton(onClick = { onExpandedChange(true) }) {
            Text(
                text = state.selectedTrip?.name ?: "Select trip",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Switch trip",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            state.trips.forEach { trip ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = buildString {
                                append(trip.name)
                                trip.destination?.let { append(" · $it") }
                            },
                        )
                    },
                    onClick = { onSelect(trip.id) },
                )
            }
            DropdownMenuItem(
                text = { Text("Create new trip") },
                onClick = onCreate,
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
        }
    }
}
