package com.tripbudget.app.ui.screens.trips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.tripbudget.app.R
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.components.DatePickerField
import com.tripbudget.app.ui.components.MoneyTextField
import com.tripbudget.app.ui.components.PrimaryCoastalButton
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditorDialog(
    existing: TripEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        budgetRupees: String,
        startDate: LocalDate,
        endDate: LocalDate,
        onResult: (String?) -> Unit,
    ) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
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
                    onSave(name, budget, startDate, endDate) { result ->
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
