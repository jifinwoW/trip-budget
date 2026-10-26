package com.tripbudget.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Swipe-down refresh that pulls the latest trip data from Supabase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePullRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onPullProgress: (fraction: Float, tracking: Boolean) -> Unit = { _, _ -> },
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    // Read during composition so each drag frame recomposes. A read inside
    // SideEffect alone does not subscribe, so the sheet only moved after release.
    val fraction = state.distanceFraction
    val tracking = !state.isAnimating && !isRefreshing
    SideEffect { onPullProgress(fraction, tracking) }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        state = state,
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.fillMaxSize()) {
            content()
        }
    }
}
