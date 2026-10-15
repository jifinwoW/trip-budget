package com.tripbudget.app.ui.screens.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.ui.components.MoneyTextField
import com.tripbudget.app.ui.components.PrimaryCoastalButton
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
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
                if (existing == null) "Add expense" else "Edit expense",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Shared fund or personal money",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Date (YYYY-MM-DD)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Payment source", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = paymentSource == PaymentSource.SHARED,
                    onClick = { paymentSource = PaymentSource.SHARED },
                    label = { Text("Shared fund") },
                    shape = RoundedCornerShape(999.dp),
                )
                FilterChip(
                    selected = paymentSource == PaymentSource.PERSONAL,
                    onClick = { paymentSource = PaymentSource.PERSONAL },
                    label = { Text("Personal") },
                    shape = RoundedCornerShape(999.dp),
                )
            }
            if (paymentSource == PaymentSource.PERSONAL) {
                if (members.isEmpty()) {
                    Text(
                        "Add members first to record a personal expense.",
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(memberExpanded) },
                            shape = RoundedCornerShape(14.dp),
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
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            PrimaryCoastalButton(
                text = "Save expense",
                onClick = {
                    val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                    if (date == null) {
                        error = "Use date format YYYY-MM-DD"
                        return@PrimaryCoastalButton
                    }
                    val memberId = when (paymentSource) {
                        PaymentSource.SHARED -> null
                        PaymentSource.PERSONAL -> {
                            val id = paidBy
                            if (id == null) {
                                error = "Select who paid"
                                return@PrimaryCoastalButton
                            }
                            id
                        }
                    }
                    onSave(title, amount, category, date, memberId, paymentSource, notes) { result ->
                        if (result == null) onDismiss() else error = result
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
