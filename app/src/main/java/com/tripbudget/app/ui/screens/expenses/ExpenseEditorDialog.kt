package com.tripbudget.app.ui.screens.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.ui.components.MoneyTextField
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditorDialog(
    members: List<MemberEntity>,
    existing: ExpenseEntity?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amountRupees: String,
        category: ExpenseCategory,
        date: LocalDate,
        paidByMemberId: Long?,
        paymentSource: PaymentSource,
        notes: String?,
        onResult: (String?) -> Unit,
    ) -> Unit,
) {
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var amount by remember(existing) {
        mutableStateOf(existing?.let { Money.paiseToRupeesString(it.amountPaise) }.orEmpty())
    }
    var category by remember(existing) { mutableStateOf(existing?.category ?: ExpenseCategory.FOOD) }
    var dateText by remember(existing) {
        mutableStateOf(
            existing?.let { LocalDate.ofEpochDay(it.dateEpochDay).toString() }
                ?: LocalDate.now().toString(),
        )
    }
    var paymentSource by remember(existing) {
        mutableStateOf(existing?.paymentSource ?: PaymentSource.SHARED)
    }
    var paidBy by remember(existing) {
        mutableStateOf(existing?.paidByMemberId ?: members.firstOrNull()?.id)
    }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var memberExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add expense" else "Edit expense") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount",
                    isError = error != null,
                    supportingText = error,
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
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Payment source", style = MaterialTheme.typography.labelLarge)
                RowChips(
                    selected = paymentSource,
                    onSelect = { paymentSource = it },
                )
                if (paymentSource == PaymentSource.PERSONAL) {
                    if (members.isEmpty()) {
                        Text(
                            text = "Add members first to record a personal expense.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = memberExpanded,
                            onExpandedChange = { memberExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = members.firstOrNull { it.id == paidBy }?.name.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Who paid") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(memberExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = memberExpanded,
                                onDismissRequest = { memberExpanded = false },
                            ) {
                                members.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text(member.name) },
                                        onClick = {
                                            paidBy = member.id
                                            memberExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                    if (date == null) {
                        error = "Use date format YYYY-MM-DD"
                        return@TextButton
                    }
                    val memberId = when (paymentSource) {
                        PaymentSource.SHARED -> null
                        PaymentSource.PERSONAL -> {
                            val id = paidBy
                            if (id == null) {
                                error = "Select who paid"
                                return@TextButton
                            }
                            id
                        }
                    }
                    onSave(
                        title,
                        amount,
                        category,
                        date,
                        memberId,
                        paymentSource,
                        notes,
                    ) { result ->
                        if (result == null) onDismiss() else error = result
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RowChips(
    selected: PaymentSource,
    onSelect: (PaymentSource) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == PaymentSource.SHARED,
            onClick = { onSelect(PaymentSource.SHARED) },
            label = { Text("Shared fund") },
        )
        FilterChip(
            selected = selected == PaymentSource.PERSONAL,
            onClick = { onSelect(PaymentSource.PERSONAL) },
            label = { Text("Personal money") },
        )
    }
}
