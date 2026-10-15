package com.tripbudget.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun EnterCascade(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visible = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier,
        enter = fadeIn(tween(420, delayMillis = delayMillis)) +
            slideInVertically(
                animationSpec = tween(480, delayMillis = delayMillis),
                initialOffsetY = { it / 10 },
            ),
    ) {
        content()
    }
}
