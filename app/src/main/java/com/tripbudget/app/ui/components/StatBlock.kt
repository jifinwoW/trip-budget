package com.tripbudget.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tripbudget.app.domain.Money

@Composable
fun StatBlock(
    label: String,
    amountPaise: Long,
    modifier: Modifier = Modifier,
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasize: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (emphasize) 12.dp else 8.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Money.formatInr(amountPaise),
            style = if (emphasize) {
                MaterialTheme.typography.displayMedium
            } else {
                MaterialTheme.typography.headlineMedium
            },
            color = amountColor,
        )
    }
}

@Composable
fun LedgerRule(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 4.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    )
}

@Composable
fun MetricCell(
    label: String,
    amountPaise: Long,
    modifier: Modifier = Modifier,
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier.padding(vertical = 10.dp, horizontal = 4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Money.formatInr(amountPaise),
            style = MaterialTheme.typography.titleLarge,
            color = amountColor,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
