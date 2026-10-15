package com.tripbudget.app.ui.screens.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.components.MoneyTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionEditorDialog(
    members: List<MemberEntity>,
    existing: ContributionEntity?,
    defaultMemberId: Long?,
    onDismiss: () -> Unit,
    onSave: (
        memberId: Long,
        amountRupees: String,
        note: String?,
        onResult: (String?) -> Unit,
    ) -> Unit,
) {
    var memberId by remember(existing, defaultMemberId) {
        mutableStateOf(existing?.memberId ?: defaultMemberId ?: members.firstOrNull()?.id)
    }
    var amount by remember(existing) {
        mutableStateOf(existing?.let { Money.paiseToRupeesString(it.amountPaise) }.orEmpty())
    }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val selectedMember = members.firstOrNull { it.id == memberId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add contribution" else "Edit contribution") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existing == null) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedMember?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Member") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            members.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.name) },
                                    onClick = {
                                        memberId = member.id
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                } else {
                    Text("Member: ${selectedMember?.name ?: "Unknown"}")
                }
                MoneyTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount paid",
                    isError = error != null,
                    supportingText = error,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = memberId
                    if (id == null) {
                        error = "Select a member"
                        return@TextButton
                    }
                    onSave(id, amount, note) { result ->
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
