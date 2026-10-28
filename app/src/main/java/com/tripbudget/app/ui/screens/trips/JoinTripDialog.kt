package com.tripbudget.app.ui.screens.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tripbudget.app.ui.components.PrimaryCoastalButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTripDialog(
    onDismiss: () -> Unit,
    onJoin: (code: String, name: String, onResult: (String?) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
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
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Join trip", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Enter your name and the invite code from your friend.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(40)
                    nameError = null
                },
                label = { Text("Your name") },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { message -> { Text(message) } },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(8)
                    codeError = null
                },
                label = { Text("Invite code") },
                singleLine = true,
                isError = codeError != null,
                supportingText = codeError?.let { message -> { Text(message) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (formError != null) {
                Text(
                    formError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            PrimaryCoastalButton(
                text = if (busy) "Joining…" else "Join trip",
                onClick = {
                    val missingName = name.isBlank()
                    val missingCode = code.length < 4
                    nameError = if (missingName) "Enter your name" else null
                    codeError = if (missingCode) "Enter a valid code" else null
                    formError = null
                    if (missingName || missingCode) return@PrimaryCoastalButton
                    busy = true
                    onJoin(code, name.trim()) { result ->
                        busy = false
                        if (result == null) {
                            onDismiss()
                            return@onJoin
                        }
                        when {
                            result.contains("name", ignoreCase = true) -> nameError = result
                            result.contains("code", ignoreCase = true) -> codeError = result
                            else -> formError = result
                        }
                    }
                },
                enabled = !busy,
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
