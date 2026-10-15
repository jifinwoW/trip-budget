package com.tripbudget.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.ui.theme.CoastalTokens
import com.tripbudget.app.ui.theme.SoftCanvas
import com.tripbudget.app.ui.theme.Teal100
import com.tripbudget.app.ui.theme.Teal700

data class CategoryVisual(
    val icon: ImageVector,
    val tint: Color,
    val background: Color,
)

fun categoryVisual(category: ExpenseCategory): CategoryVisual = when (category) {
    ExpenseCategory.FOOD -> CategoryVisual(
        Icons.Outlined.Restaurant,
        Color(0xFFC45C2A),
        Color(0xFFFFE4D4),
    )
    ExpenseCategory.FUEL -> CategoryVisual(
        Icons.Outlined.LocalGasStation,
        Color(0xFFB05A2E),
        Color(0xFFF8E0D0),
    )
    ExpenseCategory.TRANSPORT -> CategoryVisual(
        Icons.Outlined.DirectionsBus,
        Color(0xFF2A6FA8),
        Color(0xFFD7EAF7),
    )
    ExpenseCategory.ACCOMMODATION -> CategoryVisual(
        Icons.Outlined.Hotel,
        Color(0xFF5B4FA8),
        Color(0xFFE8E4F8),
    )
    ExpenseCategory.ACTIVITIES -> CategoryVisual(
        Icons.Outlined.Explore,
        Color(0xFF0B7A6E),
        Color(0xFFD5F3EE),
    )
    ExpenseCategory.OTHER -> CategoryVisual(
        Icons.Outlined.MoreHoriz,
        Color(0xFF5A6F6A),
        Color(0xFFE8EEF0),
    )
}

@Composable
fun SoftScreenCanvas(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftCanvas),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7ED9C8).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.05f),
                    radius = size.minDimension * 0.5f,
                ),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * 0.1f, size.height * 0.05f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD2B8).copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.2f),
                    radius = size.minDimension * 0.45f,
                ),
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.95f, size.height * 0.2f),
            )
        }
        content()
    }
}

@Composable
fun CoastalCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
fun CategoryIcon(
    category: ExpenseCategory,
    modifier: Modifier = Modifier,
    size: Int = 42,
) {
    val visual = categoryVisual(category)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(visual.background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = category.label,
            tint = visual.tint,
            modifier = Modifier.size((size * 0.48f).dp),
        )
    }
}

@Composable
fun ExpenseSourceChip(
    source: PaymentSource,
    modifier: Modifier = Modifier,
    payerName: String? = null,
) {
    val label = when (source) {
        PaymentSource.SHARED -> "Shared"
        PaymentSource.PERSONAL -> if (payerName != null) "Personal · $payerName" else "Personal"
    }
    val bg = when (source) {
        PaymentSource.SHARED -> Teal100
        PaymentSource.PERSONAL -> CoastalTokens.sand
    }
    val fg = when (source) {
        PaymentSource.SHARED -> Teal700
        PaymentSource.PERSONAL -> Color(0xFF8A5A2B)
    }
    SoftCategoryChip(
        text = label,
        modifier = modifier,
        containerColor = bg,
        contentColor = fg,
    )
}

@Composable
fun MemberAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Teal100),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleLarge,
            color = Teal700,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

enum class MemberPayStatus {
    SETTLED,
    OUTSTANDING,
    EXCESS,
    NONE,
}

fun memberPayStatus(outstandingPaise: Long, paidPaise: Long): MemberPayStatus = when {
    paidPaise == 0L && outstandingPaise > 0 -> MemberPayStatus.OUTSTANDING
    outstandingPaise > 0 -> MemberPayStatus.OUTSTANDING
    outstandingPaise < 0 -> MemberPayStatus.EXCESS
    paidPaise > 0 -> MemberPayStatus.SETTLED
    else -> MemberPayStatus.NONE
}

@Composable
fun MemberStatusBadge(
    status: MemberPayStatus,
    modifier: Modifier = Modifier,
) {
    val (label, bg, fg) = when (status) {
        MemberPayStatus.SETTLED -> Triple("Settled", CoastalTokens.settledBg, CoastalTokens.settled)
        MemberPayStatus.OUTSTANDING -> Triple("Outstanding", CoastalTokens.peach, Color(0xFFB05A2E))
        MemberPayStatus.EXCESS -> Triple("Excess", Teal100, Teal700)
        MemberPayStatus.NONE -> Triple("No payments", Color(0xFFE8EEF0), Color(0xFF5A6F6A))
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun PrimaryCoastalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = CoastalTokens.positive,
        ),
        modifier = modifier,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryCoastalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun LabeledValueRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(leftLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(leftValue, style = MaterialTheme.typography.titleMedium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(rightLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rightValue, style = MaterialTheme.typography.titleMedium)
        }
    }
}
