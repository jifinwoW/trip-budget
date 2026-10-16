package com.tripbudget.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripbudget.app.ui.theme.Teal600

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SoftScreenCanvas(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EnterCascade(0) {
                Text(
                    text = "TRIPBUDGET",
                    style = MaterialTheme.typography.labelLarge,
                    color = Teal600,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            EnterCascade(50) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(12.dp))
            EnterCascade(100) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(28.dp))
                EnterCascade(150) {
                    PrimaryCoastalButton(
                        text = actionLabel,
                        onClick = onAction,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    )
                }
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Spacer(Modifier.height(10.dp))
                EnterCascade(180) {
                    SecondaryCoastalButton(
                        text = secondaryActionLabel,
                        onClick = onSecondaryAction,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    )
                }
            }
        }
    }
}
