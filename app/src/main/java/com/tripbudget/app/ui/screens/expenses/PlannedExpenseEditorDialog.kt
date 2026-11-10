package com.tripbudget.app.ui.screens.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import com.tripbudget.app.data.entity.PlannedExpenseEntity
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.components.MoneyTextField
import com.tripbudget.app.ui.components.PrimaryCoastalButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedExpenseEditorDialog(
    existing: PlannedExpenseEntity?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amountRupees: String,
        category: ExpenseCategory,
        notes: String?,
        onResult: (String?) -> Unit,
    ) -> Unit,
) {
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var amount by remember(existing) {
        mutableStateOf(existing?.let { Money.paiseToRupeesString(it.amountPaise) }.orEmpty())
    }
    var category by remember(existing) {
        mutableStateOf(existing?.category ?: ExpenseCategory.FOOD)
    }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun clearFieldErrors() {
        titleError = null
        amountError = null
    }

    fun applySaveError(message: String) {
        clearFieldErrors()
        when {
            message.contains("title", ignoreCase = true) -> titleError = message
            message.contains("amount", ignoreCase = true) -> amountError = message
            else -> amountError = message
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                if (existing == null) "Plan an expense" else "Edit planned item",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Estimated budget for stays, fuel, food, or activities",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = { Text("What are you budgeting for?") },
                placeholder = { Text("e.g. Resort stay, Highway toll & fuel") },
                singleLine = true,
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            MoneyTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    amountError = null
                },
                label = "Planned Amount",
                isError = amountError != null,
                supportingText = amountError,
            )
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = category.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    ExpenseCategory.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.label) },
                            onClick = {
                                category = item
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. Booking link, per person estimate") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            PrimaryCoastalButton(
                text = if (existing == null) "Add to plan" else "Save changes",
                onClick = {
                    clearFieldErrors()
                    onSave(title, amount, category, notes) { result ->
                        if (result == null) onDismiss() else applySaveError(result)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
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

