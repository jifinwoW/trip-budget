package com.tripbudget.app.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp-based Coastal Soft spacing scale. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val section = 32.dp

    /** Capsule height only; system nav inset is added separately. */
    val floatingNavBar = 20.dp
}

/**
 * Bottom space so the last scroll item clears the floating nav capsule
 * and the system navigation / gesture bar.
 */
@Composable
fun floatingNavContentPadding(): Dp {
    return Spacing.floatingNavBar +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}
