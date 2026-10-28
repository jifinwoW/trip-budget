package com.tripbudget.app.ui.screens.plan

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.data.entity.RouteStopEntity
import com.tripbudget.app.domain.RouteStopStatus
import com.tripbudget.app.location.NextStopProximity
import com.tripbudget.app.location.RouteArrivalTracker
import com.tripbudget.app.places.PlaceSuggestion
import com.tripbudget.app.ui.components.CoastalProgressTrack
import com.tripbudget.app.ui.components.ConfirmDialog
import com.tripbudget.app.ui.components.EmptyState
import com.tripbudget.app.ui.components.ListShimmer
import com.tripbudget.app.ui.components.LivePullRefresh
import com.tripbudget.app.ui.theme.Teal600
import com.tripbudget.app.ui.theme.floatingNavContentPadding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    tripId: Long,
    viewModel: PlanViewModel,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onTrackingStarted: () -> Unit = {},
    onPinNextStopHere: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    proximity: NextStopProximity? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddPlace by remember { mutableStateOf(false) }
    var deletingStop by remember { mutableStateOf<RouteStopEntity?>(null) }

    if (onClose != null) {
        BackHandler(onBack = onClose)
    }

    val dataReady = !state.isLoading && state.tripId == tripId
    if (!dataReady) {
        ListShimmer(rowHeight = 100.dp)
        return
    }

    val tracking = state.trip?.routeTrackingActive == true
    val startsOnLabel = state.trip?.startDateEpochDay?.let { epoch ->
        val start = LocalDate.ofEpochDay(epoch)
        if (LocalDate.now().isBefore(start)) {
            start.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        } else {
            null
        }
    }
    val reachedCount = state.stops.count { it.status == RouteStopStatus.REACHED }
    val tripProximity = proximity?.takeIf { it.tripId == tripId }
    val fabBottomPad = if (onClose != null) 16.dp else floatingNavContentPadding()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPlace = true },
                containerColor = Teal600,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(bottom = fabBottomPad),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add stop")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.stops.isEmpty()) {
                LivePullRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    EmptyState(
                        title = "Plan your route",
                        message = "Add stops in order — we’ll mark them reached as you get close.",
                        actionLabel = "Add stop",
                        onAction = { showAddPlace = true },
                    )
                }
            } else {
                // Bottom sheet peeks at ~30% so the map (~70%) stays touchable for pinch-zoom.
                // Drag the sheet up to cover the map; map itself stays fixed underneath.
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val sheetPeek = maxHeight * 0.30f
                    val scaffoldState = rememberBottomSheetScaffoldState()

                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = sheetPeek,
                        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        sheetContainerColor = MaterialTheme.colorScheme.surface,
                        sheetShadowElevation = 6.dp,
                        sheetDragHandle = {
                            BottomSheetDefaults.DragHandle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            )
                        },
                        containerColor = Color(0xFF0B1412),
                        sheetContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.92f)
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = fabBottomPad + 72.dp),
                            ) {
                                RouteTrackingHeader(
                                    tracking = tracking,
                                    startsOnLabel = startsOnLabel,
                                    stopCount = state.stops.size,
                                    reachedCount = reachedCount,
                                    hasLocationPermission = hasLocationPermission,
                                    proximity = tripProximity,
                                    onStart = {
                                        if (!hasLocationPermission) {
                                            onRequestLocationPermission()
                                        }
                                        viewModel.setTrackingActive(true) {
                                            onTrackingStarted()
                                        }
                                    },
                                    onStop = { viewModel.setTrackingActive(false) },
                                    onPinHere = {
                                        if (!hasLocationPermission) {
                                            onRequestLocationPermission()
                                        } else {
                                            onPinNextStopHere()
                                        }
                                    },
                                )
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = "Stops",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                ) {
                                    itemsIndexed(state.stops, key = { _, s -> s.id }) { index, stop ->
                                        val previousReached = index > 0 &&
                                            state.stops[index - 1].status == RouteStopStatus.REACHED
                                        RoadmapStopRow(
                                            stop = stop,
                                            index = index,
                                            isFirst = index == 0,
                                            isLast = index == state.stops.lastIndex,
                                            previousReached = previousReached,
                                            onMoveUp = { viewModel.moveStop(stop, -1) },
                                            onMoveDown = { viewModel.moveStop(stop, 1) },
                                            onMarkReached = { viewModel.markReached(stop) },
                                            onDelete = { deletingStop = stop },
                                        )
                                    }
                                }
                            }
                        },
                    ) { sheetPadding ->
                        RouteOsmMap(
                            stops = state.stops,
                            showUserLocation = tracking && hasLocationPermission,
                            fullBleed = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(sheetPadding),
                        )
                    }
                }
            }

            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 4.dp)
                        .zIndex(2f)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
            }
        }
    }

    if (showAddPlace) {
        PlaceSearchSheet(
            query = state.placeQuery,
            results = state.placeResults,
            searching = state.placeSearching,
            error = state.placeError,
            onQueryChange = viewModel::onPlaceQueryChange,
            onPick = {
                viewModel.addPlace(it)
                showAddPlace = false
            },
            onDismiss = {
                showAddPlace = false
                viewModel.clearPlaceSearch()
            },
        )
    }

    deletingStop?.let { stop ->
        ConfirmDialog(
            title = "Remove stop?",
            message = "Remove “${stop.title}” from this route?",
            confirmLabel = "Remove",
            onConfirm = {
                viewModel.deleteStop(stop)
                deletingStop = null
            },
            onDismiss = { deletingStop = null },
        )
    }
}

@Composable
private fun RouteTrackingHeader(
    tracking: Boolean,
    startsOnLabel: String?,
    stopCount: Int,
    reachedCount: Int,
    hasLocationPermission: Boolean,
    proximity: NextStopProximity?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPinHere: () -> Unit,
) {
    val pinLooksWrong = proximity != null &&
        proximity.distanceMeters > RouteArrivalTracker.ARRIVAL_RADIUS_METERS
    val progress = if (stopCount <= 0) 0f else (reachedCount.toFloat() / stopCount.toFloat())
    val progressPct = (progress * 100f).roundToInt().coerceIn(0, 100)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    tracking -> "Trip in progress"
                    startsOnLabel != null -> "Starts $startsOnLabel"
                    else -> "Ready when you are"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (stopCount > 0) {
                Text(
                    text = "$progressPct%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Teal600,
                )
            }
        }
        if (stopCount > 0) {
            CoastalProgressTrack(
                progress = progress,
                fillColor = Teal600,
                height = 10.dp,
            )
            Text(
                text = "$reachedCount of $stopCount stops reached",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (tracking && proximity != null) {
            Text(
                text = "${RouteArrivalTracker.formatDistance(proximity.distanceMeters)} more to ${proximity.stopTitle}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (pinLooksWrong) {
                Text(
                    text = "Search pinned a different spot. If you’re already here, fix the pin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = when {
                    tracking && !hasLocationPermission ->
                        "Location is off — mark stops manually, or allow GPS."
                    tracking ->
                        "Getting your location…"
                    startsOnLabel != null ->
                        "GPS turns on by itself on $startsOnLabel."
                    else ->
                        "Start the trip — GPS marks stops when you’re nearby."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tracking) {
                Button(
                    onClick = onStop,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("End trip")
                }
                if (pinLooksWrong || hasLocationPermission) {
                    TextButton(onClick = onPinHere) {
                        Icon(
                            Icons.Outlined.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("I'm here")
                    }
                }
            } else {
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal600),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start trip")
                }
            }
        }
    }
}

@Composable
private fun RoadmapStopRow(
    stop: RouteStopEntity,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    previousReached: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMarkReached: () -> Unit,
    onDelete: () -> Unit,
) {
    val nodeColor = when (stop.status) {
        RouteStopStatus.REACHED -> Teal600
        RouteStopStatus.CURRENT -> MaterialTheme.colorScheme.primary
        RouteStopStatus.UPCOMING -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val incomingDone = previousReached || stop.status == RouteStopStatus.REACHED
    val outgoingDone = stop.status == RouteStopStatus.REACHED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val nodeY = 22.dp.toPx()
                if (!isFirst) {
                    drawLine(
                        color = if (incomingDone) Teal600 else lineColor,
                        start = Offset(cx, 0f),
                        end = Offset(cx, nodeY - 10.dp.toPx()),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                if (!isLast) {
                    val effect = if (outgoingDone) {
                        null
                    } else {
                        PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    }
                    drawLine(
                        color = if (outgoingDone) Teal600 else lineColor,
                        start = Offset(cx, nodeY + 10.dp.toPx()),
                        end = Offset(cx, size.height),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = effect,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(nodeColor),
                contentAlignment = Alignment.Center,
            ) {
                when (stop.status) {
                    RouteStopStatus.REACHED -> Icon(
                        Icons.Default.Check,
                        contentDescription = "Reached",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    RouteStopStatus.CURRENT -> Icon(
                        Icons.Outlined.MyLocation,
                        contentDescription = "Current",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    RouteStopStatus.UPCOMING -> Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, bottom = 14.dp, end = 0.dp)
                .padding(vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stop.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = when (stop.status) {
                        RouteStopStatus.REACHED -> "Reached"
                        RouteStopStatus.CURRENT -> "Next"
                        RouteStopStatus.UPCOMING -> "Upcoming"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!stop.subtitle.isNullOrBlank()) {
                Text(
                    text = stop.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                }
                if (stop.status != RouteStopStatus.REACHED) {
                    TextButton(onClick = onMarkReached) { Text("Reached") }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceSearchSheet(
    query: String,
    results: List<PlaceSuggestion>,
    searching: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onPick: (PlaceSuggestion) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboard = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Add a stop",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Search OpenStreetMap places — free, no Google Maps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                placeholder = { Text("e.g. Munnar, Kochi…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            )
            when {
                searching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Teal600)
                    }
                }
                error != null -> {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                results.isEmpty() && query.trim().length >= 2 -> {
                    Text(
                        "No places found. Try another spelling.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    results.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    keyboard?.hide()
                                    onPick(suggestion)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = Teal600,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    suggestion.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (!suggestion.subtitle.isNullOrBlank()) {
                                    Text(
                                        suggestion.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Host helper: request fine/coarse location when starting route tracking. */
@Composable
fun rememberRouteLocationPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
) = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
) { result ->
    val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
        result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    onResult(granted)
}
