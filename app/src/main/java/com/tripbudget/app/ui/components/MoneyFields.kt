package com.tripbudget.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun MoneyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() || it == '.' }
            val parts = filtered.split('.')
            val normalized = when {
                parts.size <= 1 -> filtered
                else -> parts[0] + "." + parts.drop(1).joinToString("").take(2)
            }
            onValueChange(normalized)
        },
        label = { Text(label) },
        prefix = { Text("₹ ") },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}
