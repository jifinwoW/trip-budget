package com.tripbudget.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Home),
    EXPENSES("expenses", "Expenses", Icons.Default.AccountBalanceWallet),
    MEMBERS("members", "Members", Icons.Default.Group),
    SETTINGS("settings", "Settings", Icons.Default.Settings),
}
