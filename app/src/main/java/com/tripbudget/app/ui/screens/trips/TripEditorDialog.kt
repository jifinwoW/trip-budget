package com.tripbudget.app.ui.screens.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.components.MoneyTextField

@Composable
fun TripEditorDialog(
    existing: TripEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, destination: String?, budgetRupees: String, onResult: (String?) -> Unit) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var destination by remember(existing) { mutableStateOf(existing?.destination.orEmpty()) }
    var budget by remember(existing) {
        mutableStateOf(existing?.let { Money.paiseToRupeesString(it.plannedBudgetPaise) }.orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New trip" else "Edit trip") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Trip name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = "Planned budget",
                    isError = error != null,
                    supportingText = error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name, destination, budget) { result ->
                        if (result == null) onDismiss() else error = result
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
