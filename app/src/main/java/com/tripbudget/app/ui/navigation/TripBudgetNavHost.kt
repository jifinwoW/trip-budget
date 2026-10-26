package com.tripbudget.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripbudget.app.R
import com.tripbudget.app.cloud.AuthRepository
import com.tripbudget.app.cloud.SyncEngine
import com.tripbudget.app.data.HeaderWeatherPreference
import com.tripbudget.app.data.SyncPreferences
import com.tripbudget.app.data.ThemePreferences
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.TripDates
import com.tripbudget.app.location.RouteArrivalTracker
import com.tripbudget.app.places.DeviceLocationWeather
import com.tripbudget.app.places.OpenMeteoWeather
import com.tripbudget.app.places.PlaceWeather
import com.tripbudget.app.places.RainIntensity
import com.tripbudget.app.places.WeatherKind
import com.tripbudget.app.ui.TripSelectionUiState
import com.tripbudget.app.ui.TripSelectionViewModel
import com.tripbudget.app.ui.components.ConfirmDialog
import com.tripbudget.app.ui.components.EmptyState
import com.tripbudget.app.ui.components.HeaderWeatherAtmosphere
import com.tripbudget.app.ui.components.LivePullRefresh
import com.tripbudget.app.ui.components.LoadingContent
import com.tripbudget.app.ui.components.headerWashColors
import com.tripbudget.app.ui.screens.auth.AuthBottomSheet
import com.tripbudget.app.ui.screens.dashboard.DashboardScreen
import com.tripbudget.app.ui.screens.dashboard.DashboardViewModel
import com.tripbudget.app.ui.screens.expenses.ExpensesScreen
import com.tripbudget.app.ui.screens.expenses.ExpensesViewModel
import com.tripbudget.app.ui.screens.members.MembersScreen
import com.tripbudget.app.ui.screens.members.MembersViewModel
import com.tripbudget.app.ui.screens.plan.PlanScreen
import com.tripbudget.app.ui.screens.plan.PlanViewModel
import com.tripbudget.app.ui.screens.plan.rememberRouteLocationPermissionLauncher
import com.tripbudget.app.ui.screens.settings.SettingsScreen
import com.tripbudget.app.ui.screens.settings.SettingsViewModel
import com.tripbudget.app.ui.screens.trips.JoinTripDialog
import com.tripbudget.app.ui.screens.trips.ShareTripDialog
import com.tripbudget.app.ui.screens.trips.TripEditorDialog
import com.tripbudget.app.ui.theme.CoastalTokens
import com.tripbudget.app.ui.theme.LoraFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlin.math.abs

/** Toolbar row under the status bar. ~20% taller so header weather art isn’t cropped so hard. */
private val HeaderToolbarHeight = 67.dp

/** Extra sky below the toolbar so hero rounded corners sit on weather, not white. */
private val HeaderWeatherUnderHero = 43.dp

/** Keep in sync with [navSlideTransition] duration. */
private const val NavSlideDurationMs = 340

/** Top corners of a settled tab sheet. Straight while the sheet is still rising. */
private val PageSheetCorner = 24.dp

private const val SectionRiseMs = 280

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripBudgetNavHost(
    repository: TripRepository,
    themePreferences: ThemePreferences,
    authRepository: AuthRepository,
    syncEngine: SyncEngine,
    syncPreferences: SyncPreferences,
    routeArrivalTracker: RouteArrivalTracker? = null,
) {
    val tripSelectionVm: TripSelectionViewModel = viewModel(
        factory = TripSelectionViewModel.factory(repository, syncEngine, authRepository),
    )
    val tripIdFlow = remember(tripSelectionVm) {
        tripSelectionVm.uiState.map { it.selectedTripId }
    }
    // Always create ViewModels unconditionally (Compose rule); they stay idle until a trip exists.
    val dashboardVm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(repository, tripIdFlow),
    )
    val planVm: PlanViewModel = viewModel(
        factory = PlanViewModel.factory(repository, tripIdFlow),
    )
    val expensesVm: ExpensesViewModel = viewModel(
        factory = ExpensesViewModel.factory(repository, tripIdFlow),
    )
    val membersVm: MembersViewModel = viewModel(
        factory = MembersViewModel.factory(repository, tripIdFlow),
    )
    var hasLocationPermission by remember {
        mutableStateOf(routeArrivalTracker?.hasLocationPermission() == true)
    }
    val locationPermissionLauncher = rememberRouteLocationPermissionLauncher { granted ->
        hasLocationPermission = granted || routeArrivalTracker?.hasLocationPermission() == true
        if (hasLocationPermission) {
            routeArrivalTracker?.onPermissionGranted()
        }
    }
    val emptyProximity = remember {
        kotlinx.coroutines.flow.MutableStateFlow<com.tripbudget.app.location.NextStopProximity?>(null)
    }
    val routeProximity by (routeArrivalTracker?.proximity ?: emptyProximity)
        .collectAsStateWithLifecycle()


    val settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            themePreferences = themePreferences,
            authRepository = authRepository,
            syncEngine = syncEngine,
            syncPreferences = syncPreferences,
            tripRepository = repository,
        ),
    )

    val tripState by tripSelectionVm.uiState.collectAsStateWithLifecycle()
    val authUser by tripSelectionVm.authUser.collectAsStateWithLifecycle()
    val isRefreshing by tripSelectionVm.isRefreshing.collectAsStateWithLifecycle()
    val authResolving by authRepository.isAuthResolving.collectAsStateWithLifecycle(initialValue = true)
    val pages = remember { AppDestination.entries }
    var currentPage by remember { mutableIntStateOf(0) }

    var showCreateTrip by remember { mutableStateOf(false) }
    var showEditTrip by remember { mutableStateOf(false) }
    var showDeleteTrip by remember { mutableStateOf(false) }
    var showShareTrip by remember { mutableStateOf(false) }
    var showJoinTrip by remember { mutableStateOf(false) }
    var showAuthForCreate by remember { mutableStateOf(false) }
    var showSignIn by remember { mutableStateOf(false) }
    var showRoutePlan by remember { mutableStateOf(false) }
    var tripMenuExpanded by remember { mutableStateOf(false) }

    // Only react to settled auth. Never wipe local data here — that race destroyed
    // trips when the app resumed and session briefly looked empty.
    LaunchedEffect(authUser?.id, authResolving) {
        if (authResolving) return@LaunchedEffect
        if (authUser == null) {
            tripSelectionVm.stopLiveSync()
            showCreateTrip = false
            showEditTrip = false
            showDeleteTrip = false
            showShareTrip = false
            showAuthForCreate = false
            showSignIn = false
            showJoinTrip = false
            showRoutePlan = false
            tripMenuExpanded = false
        } else {
            tripSelectionVm.startLiveSync()
        }
    }

    fun goToPage(destination: AppDestination) {
        val index = pages.indexOf(destination)
        if (index >= 0) currentPage = index
    }

    fun requestCreateTrip() {
        if (tripSelectionVm.hasEmailAccount()) {
            showCreateTrip = true
        } else {
            showAuthForCreate = true
        }
    }

    when {
        authResolving || tripState.isLoading -> {
            LoadingContent()
        }
        authUser == null -> {
            EmptyState(
                title = stringResource(R.string.brand_name),
                message = "Sign in to open your trips, or join a friend’s trip with a code — no account needed.",
                actionLabel = "Sign in",
                onAction = { showSignIn = true },
                secondaryActionLabel = "Join trip",
                onSecondaryAction = { showJoinTrip = true },
            )
            if (showJoinTrip) {
                JoinTripDialog(
                    onDismiss = { showJoinTrip = false },
                    onJoin = { code, onResult -> tripSelectionVm.joinTrip(code, onResult) },
                )
            }
            if (showSignIn) {
                AuthBottomSheet(
                    authRepository = authRepository,
                    title = "Sign in",
                    message = "Sign in to sync and manage your trips.",
                    onDismiss = { showSignIn = false },
                    onAuthenticated = {
                        showSignIn = false
                        tripSelectionVm.refreshFromCloud()
                    },
                )
            }
        }
        tripState.trips.isEmpty() -> {
            LivePullRefresh(
                isRefreshing = isRefreshing,
                onRefresh = { tripSelectionVm.refreshFromCloud(force = true) },
            ) {
                EmptyState(
                    title = stringResource(R.string.brand_name),
                    message = "Create your own trip, or join a friend’s trip with a code.",
                    actionLabel = "Create trip",
                    onAction = { requestCreateTrip() },
                    secondaryActionLabel = "Join trip",
                    onSecondaryAction = { showJoinTrip = true },
                )
            }
            if (showJoinTrip) {
                JoinTripDialog(
                    onDismiss = { showJoinTrip = false },
                    onJoin = { code, onResult -> tripSelectionVm.joinTrip(code, onResult) },
                )
            }
            if (showAuthForCreate) {
                AuthBottomSheet(
                    authRepository = authRepository,
                    onDismiss = { showAuthForCreate = false },
                    onAuthenticated = {
                        showAuthForCreate = false
                        showCreateTrip = true
                        tripSelectionVm.refreshFromCloud()
                    },
                )
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
        }
        else -> {
            val selectedTrip = tripState.selectedTrip
            if (selectedTrip == null) {
                LoadingContent()
            } else {
            val context = LocalContext.current
            val headerWeatherPref by themePreferences.headerWeather
                .collectAsStateWithLifecycle(HeaderWeatherPreference.LIVE)
            var liveWeather by remember { mutableStateOf<PlaceWeather?>(null) }
            var weatherRefreshKey by remember { mutableIntStateOf(0) }
            // Refetch on permission, tab change, Header weather pref (e.g. back to Live),
            // and pull-to-refresh (via weatherRefreshKey).
            LaunchedEffect(
                hasLocationPermission,
                currentPage,
                headerWeatherPref,
                weatherRefreshKey,
            ) {
                liveWeather = if (hasLocationPermission) {
                    DeviceLocationWeather.fetch(context)
                } else {
                    null
                }
            }
            fun refreshCloudAndWeather() {
                tripSelectionVm.refreshFromCloud(force = true)
                weatherRefreshKey += 1
            }
            // Live: show sunny until location weather arrives, then Crossfade to real kind.
            val headerWeatherKind = when (headerWeatherPref) {
                HeaderWeatherPreference.LIVE -> liveWeather?.kind ?: WeatherKind.SUNNY
                else -> headerWeatherPref.toWeatherKind()
            }
            val headerRainIntensity = when {
                headerWeatherKind == WeatherKind.STORM -> RainIntensity.STORM
                headerWeatherKind == WeatherKind.RAIN &&
                    headerWeatherPref == HeaderWeatherPreference.LIVE -> {
                    OpenMeteoWeather.rainIntensityFromCode(liveWeather?.weatherCode)
                }
                headerWeatherKind == WeatherKind.RAIN -> RainIntensity.MEDIUM
                else -> RainIntensity.MEDIUM
            }
            val statusBarPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            // Same weather height on every tab — Dashboard and the other pages share this strip.
            val weatherArtHeight = statusBarPad + HeaderToolbarHeight + HeaderWeatherUnderHero
            // Incoming page sits under the full header during the slide, then tucks up
            // to the hero line once the page has arrived.
            var settledPage by remember { mutableIntStateOf(currentPage) }
            var pullFraction by remember { mutableFloatStateOf(0f) }
            // Opening the app refreshes on its own and drives the pull indicator to 1.
            // Keep the sheet still until that indicator is hidden again.
            var ignorePullUntilHidden by remember { mutableStateOf(isRefreshing) }
            LaunchedEffect(currentPage) {
                pullFraction = 0f
                if (settledPage == currentPage) return@LaunchedEffect
                delay(NavSlideDurationMs.toLong())
                settledPage = currentPage
            }
            LaunchedEffect(isRefreshing, pullFraction) {
                if (isRefreshing) {
                    ignorePullUntilHidden = true
                } else if (pullFraction <= 0f) {
                    ignorePullUntilHidden = false
                }
            }
            val pullActive = !isRefreshing && !ignorePullUntilHidden
            val pullVisual by animateFloatAsState(
                targetValue = if (pullActive) pullFraction.coerceIn(0f, 1f) else 0f,
                animationSpec = if (pullActive) snap() else tween(SectionRiseMs, easing = FastOutSlowInEasing),
                label = "pull_sheet",
            )
            val onPullProgress: (Float, Boolean) -> Unit = { fraction, _ ->
                pullFraction = fraction
            }
            Box(modifier = Modifier.fillMaxSize()) {
            // Weather sky behind toolbar; Crossfade for Header weather changes in Settings.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(weatherArtHeight)
                    .align(Alignment.TopCenter)
                    .clipToBounds()
                    .zIndex(0f),
            ) {
                Crossfade(
                    targetState = headerWeatherKind to headerRainIntensity,
                    animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                    label = "header_weather_kind",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(weatherArtHeight)
                        .align(Alignment.TopCenter),
                ) { (kind, intensity) ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    headerWashColors(kind, CoastalTokens.isDark),
                                ),
                            ),
                    ) {
                        HeaderWeatherAtmosphere(
                            kind = kind,
                            isDark = CoastalTokens.isDark,
                            rainIntensity = intensity,
                            // Impact in the under-hero peek (below the toolbar chip).
                            impactHorizonRatio = 0.80f,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CoastalAppHeader(
                        state = tripState,
                        weatherCaption = when (headerWeatherPref) {
                            HeaderWeatherPreference.LIVE -> {
                                val weather = liveWeather
                                when {
                                    !hasLocationPermission || weather == null -> null
                                    else -> {
                                        val temp = weather.temperatureC?.let { "${it.toInt()}°" }
                                        listOfNotNull(weather.label, temp).joinToString(" · ")
                                    }
                                }
                            }
                            else -> headerWeatherPref.label()
                        },
                        tripMenuExpanded = tripMenuExpanded,
                        onTripMenuExpandedChange = { tripMenuExpanded = it },
                        onSelectTrip = {
                            tripSelectionVm.selectTrip(it)
                            tripMenuExpanded = false
                        },
                        onCreateTrip = {
                            tripMenuExpanded = false
                            requestCreateTrip()
                        },
                        onJoinTrip = {
                            tripMenuExpanded = false
                            showJoinTrip = true
                        },
                        onShareTrip = {
                            tripMenuExpanded = false
                            showShareTrip = true
                        },
                        onEditTrip = {
                            tripMenuExpanded = false
                            showEditTrip = true
                        },
                        onDeleteTrip = {
                            tripMenuExpanded = false
                            showDeleteTrip = true
                        },
                        canDeleteTrip = tripState.selectedIsOwned,
                    )
                },
                modifier = Modifier.zIndex(1f),
            ) { padding ->
                BoxWithConstraints(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                ) {
                    val pageWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                    var dragDistance by remember { mutableFloatStateOf(0f) }
                    AnimatedContent(
                        targetState = currentPage,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(currentPage, pageWidthPx) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragDistance = 0f },
                                    onHorizontalDrag = { _, amount ->
                                        dragDistance += amount
                                    },
                                    onDragEnd = {
                                        val threshold = pageWidthPx * 0.22f
                                        when {
                                            dragDistance < -threshold &&
                                                currentPage < pages.lastIndex -> {
                                                currentPage += 1
                                            }
                                            dragDistance > threshold && currentPage > 0 -> {
                                                currentPage -= 1
                                            }
                                        }
                                        dragDistance = 0f
                                    },
                                    onDragCancel = { dragDistance = 0f },
                                )
                            },
                        transitionSpec = {
                            navSlideTransition(forward = targetState > initialState)
                        },
                        label = "main_nav_slide",
                    ) { page ->
                        val sectionSettled = page == settledPage
                        val sectionMotion = tween<Dp>(durationMillis = SectionRiseMs, easing = FastOutSlowInEasing)
                        val riseTop by animateDpAsState(
                            targetValue = if (sectionSettled) 0.dp else HeaderWeatherUnderHero,
                            animationSpec = sectionMotion,
                            label = "section_slide_up",
                        )
                        val riseCorner by animateDpAsState(
                            targetValue = if (sectionSettled) PageSheetCorner else 0.dp,
                            animationSpec = sectionMotion,
                            label = "section_corner",
                        )
                        val pull = if (page == currentPage) pullVisual.coerceIn(0f, 1f) else 0f
                        // Stop at the bottom of the header weather, not further down the page.
                        val sectionTop = riseTop + HeaderWeatherUnderHero * pull
                        val sectionCorner = riseCorner * (1f - pull)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = sectionTop),
                        ) {
                        when (pages[page]) {
                            AppDestination.DASHBOARD -> {
                                DashboardScreen(
                                    trip = selectedTrip,
                                    viewModel = dashboardVm,
                                    headerWeatherKind = headerWeatherKind,
                                    weatherUnderHero = HeaderWeatherUnderHero,
                                    isRefreshing = isRefreshing,
                                    onRefresh = { refreshCloudAndWeather() },
                                    onEditTrip = { showEditTrip = true },
                                    onViewAllExpenses = {
                                        goToPage(AppDestination.EXPENSES)
                                    },
                                    onOpenRoutePlan = { showRoutePlan = true },
                                )
                            }
                            AppDestination.EXPENSES -> {
                                RoundedPageSheet(cornerRadius = sectionCorner) {
                                    ExpensesScreen(
                                        tripId = selectedTrip.id,
                                        viewModel = expensesVm,
                                        isRefreshing = isRefreshing,
                                        onRefresh = { refreshCloudAndWeather() },
                                        onPullProgress = onPullProgress,
                                    )
                                }
                            }
                            AppDestination.MEMBERS -> {
                                RoundedPageSheet(cornerRadius = sectionCorner) {
                                    MembersScreen(
                                        tripId = selectedTrip.id,
                                        viewModel = membersVm,
                                        isRefreshing = isRefreshing,
                                        onRefresh = { refreshCloudAndWeather() },
                                        onPullProgress = onPullProgress,
                                    )
                                }
                            }
                            AppDestination.SETTINGS -> {
                                RoundedPageSheet(cornerRadius = sectionCorner) {
                                    SettingsScreen(
                                        viewModel = settingsVm,
                                        liveWeather = liveWeather,
                                        hasLocationPermission = hasLocationPermission,
                                        isRefreshing = isRefreshing,
                                        onRefresh = { refreshCloudAndWeather() },
                                        onPullProgress = onPullProgress,
                                    )
                                }
                            }
                        }
                        }
                    }
                }
            }

            if (!showRoutePlan) {
                SlidingBottomBar(
                    destinations = pages,
                    selectedIndex = currentPage,
                    onSelect = { currentPage = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(2f),
                )
            }

            if (showRoutePlan) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .zIndex(10f),
                ) {
                    PlanScreen(
                        tripId = selectedTrip.id,
                        viewModel = planVm,
                        hasLocationPermission = hasLocationPermission,
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        },
                        onTrackingStarted = {
                            routeArrivalTracker?.nudgeCheck()
                        },
                        onPinNextStopHere = {
                            routeArrivalTracker?.pinNextStopToHere(selectedTrip.id)
                        },
                        onClose = { showRoutePlan = false },
                        proximity = routeProximity,
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshCloudAndWeather() },
                    )
                }
            }
            }

            if (showAuthForCreate) {
                AuthBottomSheet(
                    authRepository = authRepository,
                    onDismiss = { showAuthForCreate = false },
                    onAuthenticated = {
                        showAuthForCreate = false
                        showCreateTrip = true
                    },
                )
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
            if (showDeleteTrip && tripState.selectedIsOwned) {
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
            if (showShareTrip) {
                ShareTripDialog(
                    trip = selectedTrip,
                    onPrepareCode = { trip, onResult ->
                        tripSelectionVm.prepareShareCode(trip, onResult)
                    },
                    onDismiss = { showShareTrip = false },
                )
            }
            if (showJoinTrip) {
                JoinTripDialog(
                    onDismiss = { showJoinTrip = false },
                    onJoin = { code, onResult -> tripSelectionVm.joinTrip(code, onResult) },
                )
            }
            }
        }
    }
}

/**
 * Coastal top bar: brand | trip switcher (trips + trip actions).
 */
@Composable
private fun CoastalAppHeader(
    state: TripSelectionUiState,
    weatherCaption: String?,
    tripMenuExpanded: Boolean,
    onTripMenuExpandedChange: (Boolean) -> Unit,
    onSelectTrip: (Long) -> Unit,
    onCreateTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onShareTrip: () -> Unit,
    onEditTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
    canDeleteTrip: Boolean,
) {
    val accent = CoastalTokens.accent
    val selectedTrip = state.selectedTrip
    val tripInitial = selectedTrip?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val chevronRotation by animateFloatAsState(
        targetValue = if (tripMenuExpanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "trip_chevron",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(HeaderToolbarHeight)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Image(
                    painter = painterResource(R.drawable.ic_brand_chilavu),
                    contentDescription = stringResource(R.string.brand_name),
                    modifier = Modifier
                        .height(if (weatherCaption.isNullOrBlank()) 32.dp else 26.dp)
                        .aspectRatio(913f / 320f),
                    contentScale = ContentScale.Fit,
                )
                if (!weatherCaption.isNullOrBlank()) {
                    Text(
                        text = weatherCaption,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box {
                // Glass chip — low opacity so header weather art stays visible underneath.
                Row(
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .shadow(3.dp, RoundedCornerShape(18.dp), clip = false)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.22f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.28f),
                            RoundedCornerShape(18.dp),
                        )
                        .clickable { onTripMenuExpandedChange(true) }
                        .padding(start = 6.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tripInitial,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = LoraFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = selectedTrip?.name ?: "Select trip",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = selectedTrip?.destination?.takeIf { it.isNotBlank() }
                                ?: if (state.selectedIsOwned) "Your trip" else "Joined trip",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "Trip menu",
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(chevronRotation),
                        )
                    }
                }
                DropdownMenu(
                    expanded = tripMenuExpanded,
                    onDismissRequest = { onTripMenuExpandedChange(false) },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = CoastalTokens.card,
                    shadowElevation = 10.dp,
                    modifier = Modifier.widthIn(min = 268.dp, max = 300.dp),
                ) {
                    Text(
                        "Switch trip",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CoastalTokens.muted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (state.ownedTrips.isNotEmpty()) {
                        TripMenuSectionLabel(title = "Yours", tint = accent)
                        state.ownedTrips.forEach { trip ->
                            DropdownMenuItem(
                                text = {
                                    TripMenuLabel(
                                        trip = trip,
                                        badge = "Owner",
                                        owned = true,
                                        selected = trip.id == selectedTrip?.id,
                                    )
                                },
                                onClick = { onSelectTrip(trip.id) },
                            )
                        }
                    }
                    if (state.joinedTrips.isNotEmpty()) {
                        TripMenuSectionLabel(title = "Joined", tint = CoastalTokens.muted)
                        state.joinedTrips.forEach { trip ->
                            DropdownMenuItem(
                                text = {
                                    TripMenuLabel(
                                        trip = trip,
                                        badge = "Joined",
                                        owned = false,
                                        selected = trip.id == selectedTrip?.id,
                                    )
                                },
                                onClick = { onSelectTrip(trip.id) },
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                        color = CoastalTokens.softFillAlt,
                    )
                    TripMenuActionRow(
                        icon = Icons.Default.Add,
                        label = "Create trip",
                        onClick = onCreateTrip,
                    )
                    TripMenuActionRow(
                        icon = Icons.Default.GroupAdd,
                        label = "Join with code",
                        onClick = onJoinTrip,
                    )
                    TripMenuActionRow(
                        icon = Icons.Default.Share,
                        label = "Share trip",
                        onClick = onShareTrip,
                    )
                    TripMenuActionRow(
                        icon = Icons.Default.Edit,
                        label = "Edit trip",
                        onClick = onEditTrip,
                    )
                    if (canDeleteTrip) {
                        TripMenuActionRow(
                            icon = Icons.Default.Delete,
                            label = "Delete trip",
                            onClick = onDeleteTrip,
                            destructive = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripMenuSectionLabel(
    title: String,
    tint: Color,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = tint,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun TripMenuActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val tint = if (destructive) CoastalTokens.danger else CoastalTokens.ink
    val iconBg = if (destructive) CoastalTokens.dangerSoft else CoastalTokens.softFill
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = tint,
                )
            }
        },
        onClick = onClick,
    )
}

/** Tab body under the header. Top corners ease from straight to rounded as it rises. */
@Composable
private fun RoundedPageSheet(
    cornerRadius: Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
            .background(CoastalTokens.canvas),
    ) {
        content()
    }
}

/** Navigation-style horizontal slide (enter full width, exit ~1/3). */
private fun navSlideTransition(forward: Boolean): ContentTransform {
    val duration = NavSlideDurationMs
    val easing = FastOutSlowInEasing
    return if (forward) {
        slideInHorizontally(
            animationSpec = tween(duration, easing = easing),
            initialOffsetX = { fullWidth -> fullWidth },
        ) + fadeIn(animationSpec = tween(duration / 2, easing = easing)) togetherWith
            slideOutHorizontally(
                animationSpec = tween(duration, easing = easing),
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
            ) + fadeOut(animationSpec = tween(duration / 2, easing = easing))
    } else {
        slideInHorizontally(
            animationSpec = tween(duration, easing = easing),
            initialOffsetX = { fullWidth -> -fullWidth },
        ) + fadeIn(animationSpec = tween(duration / 2, easing = easing)) togetherWith
            slideOutHorizontally(
                animationSpec = tween(duration, easing = easing),
                targetOffsetX = { fullWidth -> fullWidth / 3 },
            ) + fadeOut(animationSpec = tween(duration / 2, easing = easing))
    }
}

@Composable
private fun TripMenuLabel(
    trip: TripEntity,
    badge: String,
    owned: Boolean,
    selected: Boolean,
) {
    val initial = trip.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Row(    
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    if (owned) CoastalTokens.tealSoft else CoastalTokens.softFill,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initial,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (owned) CoastalTokens.tealSoftFg else CoastalTokens.muted,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trip.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CoastalTokens.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                trip.destination?.takeIf { it.isNotBlank() }?.let { dest ->
                    Icon(
                        Icons.Outlined.Place,
                        contentDescription = null,
                        tint = CoastalTokens.muted,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        dest,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoastalTokens.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (owned) CoastalTokens.tealSoftFg else CoastalTokens.muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (owned) CoastalTokens.tealSoft else CoastalTokens.softFill,
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            TripDates.formatRange(trip)?.let { range ->
                Text(
                    text = range,
                    style = MaterialTheme.typography.labelSmall,
                    color = CoastalTokens.muted.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = CoastalTokens.accent,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Spacer(Modifier.width(18.dp))
        }
    }
}
