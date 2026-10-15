package com.tripbudget.app.ui.screens.members

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
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.components.MoneyTextField

@Composable
fun MemberEditorDialog(
    existing: MemberEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, expectedRupees: String, onResult: (String?) -> Unit) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var expected by remember(existing) {
        mutableStateOf(
            existing?.let { Money.paiseToRupeesString(it.expectedContributionPaise) }.orEmpty(),
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add member" else "Edit member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyTextField(
                    value = expected,
                    onValueChange = { expected = it },
                    label = "Expected contribution",
                    isError = error != null,
                    supportingText = error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name, expected) { result ->
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
