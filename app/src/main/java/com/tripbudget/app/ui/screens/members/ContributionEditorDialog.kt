package com.tripbudget.app.ui.screens.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.components.MoneyTextField
import com.tripbudget.app.ui.components.PrimaryCoastalButton

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
                .padding(bottom = 28.dp),
        ) {
            Text(
                if (existing == null) "Record contribution" else "Edit contribution",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Add money to the shared trip fund",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                        shape = RoundedCornerShape(14.dp),
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
                Text(
                    "Member: ${selectedMember?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.titleMedium,
                )
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
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            PrimaryCoastalButton(
                text = "Save payment",
                onClick = {
                    val id = memberId
                    if (id == null) {
                        error = "Select a member"
                        return@PrimaryCoastalButton
                    }
                    onSave(id, amount, note) { result ->
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
