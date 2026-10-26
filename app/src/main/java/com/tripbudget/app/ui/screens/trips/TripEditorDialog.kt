package com.tripbudget.app.ui.screens.trips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tripbudget.app.R
import com.tripbudget.app.cloud.friendlyCloudError
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.places.PhotonPlaceSearch
import com.tripbudget.app.places.PlaceSuggestion
import com.tripbudget.app.places.TripPlace
import com.tripbudget.app.ui.components.DatePickerField
import com.tripbudget.app.ui.components.MoneyTextField
import com.tripbudget.app.ui.components.PrimaryCoastalButton
import com.tripbudget.app.ui.theme.Teal600
import java.time.LocalDate
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditorDialog(
    existing: TripEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        place: TripPlace?,
        budgetRupees: String,
        startDate: LocalDate,
        endDate: LocalDate,
        onResult: (String?) -> Unit,
    ) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var placeQuery by remember(existing) {
        mutableStateOf(
            if (existing?.placeLat != null && existing.placeLng != null) {
                existing.destination.orEmpty()
            } else {
                ""
            },
        )
    }
    var pickedPlace by remember(existing) {
        mutableStateOf(
            if (existing?.placeLat != null && existing.placeLng != null && !existing.destination.isNullOrBlank()) {
                TripPlace(existing.destination, existing.placeLat, existing.placeLng)
            } else {
                null
            },
        )
    }
    var placeResults by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var placeSearching by remember { mutableStateOf(false) }
    var placeError by remember { mutableStateOf<String?>(null) }
    val placeSearch = remember { PhotonPlaceSearch() }
    var budget by remember(existing) {
        mutableStateOf(existing?.let { Money.paiseToRupeesString(it.plannedBudgetPaise) }.orEmpty())
    }
    var startDate by remember(existing) {
        mutableStateOf(
            existing?.startDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now(),
        )
    }
    var endDate by remember(existing) {
        mutableStateOf(
            existing?.endDateEpochDay?.let { LocalDate.ofEpochDay(it) }
                ?: LocalDate.now().plusDays(6),
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(placeQuery, pickedPlace?.name) {
        val query = placeQuery.trim()
        if (pickedPlace != null && query == pickedPlace?.name) {
            placeResults = emptyList()
            placeSearching = false
            placeError = null
            return@LaunchedEffect
        }
        if (query.length < 2) {
            placeResults = emptyList()
            placeSearching = false
            placeError = null
            return@LaunchedEffect
        }
        delay(350)
        placeSearching = true
        placeError = null
        runCatching { placeSearch.search(query) }
            .onSuccess { placeResults = it }
            .onFailure { err ->
                placeResults = emptyList()
                placeError = friendlyCloudError(err, "Couldn't search places. Check your connection.")
            }
        placeSearching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(18.dp)),
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
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                            ),
                        ),
                )
                Text(
                    text = if (existing == null) "New trip" else "Edit trip",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Trip name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = placeQuery,
                onValueChange = {
                    placeQuery = it
                    if (pickedPlace != null && it.trim() != pickedPlace?.name) {
                        pickedPlace = null
                    }
                },
                label = { Text("Place") },
                placeholder = { Text("Search a city or town") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (placeQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            placeQuery = ""
                            pickedPlace = null
                            placeResults = emptyList()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear place")
                        }
                    }
                },
                supportingText = {
                    Text(
                        if (pickedPlace != null) "Header weather follows this place" else "Optional",
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (placeSearching) {
                Text(
                    "Searching…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            placeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            placeResults.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val placeName = listOfNotNull(
                                suggestion.title,
                                suggestion.subtitle?.takeIf { part -> part.isNotBlank() },
                            ).joinToString(", ")
                            pickedPlace = TripPlace(placeName, suggestion.lat, suggestion.lng)
                            placeQuery = placeName
                            placeResults = emptyList()
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = Teal600,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            suggestion.title,
                            style = MaterialTheme.typography.bodyMedium,
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
            DatePickerField(
                value = startDate,
                onValueChange = { picked ->
                    startDate = picked
                    dateError = null
                    if (endDate.isBefore(picked)) {
                        endDate = picked
                    }
                },
                label = "Start date",
            )
            DatePickerField(
                value = endDate,
                onValueChange = {
                    endDate = it
                    dateError = null
                },
                label = "End date",
                isError = dateError != null,
                supportingText = dateError,
                minDate = startDate,
            )
            MoneyTextField(
                value = budget,
                onValueChange = { budget = it },
                label = "Planned budget",
                isError = error != null,
                supportingText = error,
            )
            Spacer(Modifier.height(4.dp))
            PrimaryCoastalButton(
                text = "Save trip",
                onClick = {
                    if (endDate.isBefore(startDate)) {
                        dateError = "End date must be on or after start"
                        return@PrimaryCoastalButton
                    }
                    onSave(name, pickedPlace, budget, startDate, endDate) { result ->
                        when {
                            result == null -> onDismiss()
                            result.contains("date", ignoreCase = true) -> dateError = result
                            else -> error = result
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Cancel")
            }
        }
    }
}
