package com.tripbudget.app.di

import android.content.Context
import com.tripbudget.app.data.ThemePreferences
import com.tripbudget.app.data.TripBudgetDatabase
import com.tripbudget.app.data.TripRepository

class AppContainer(context: Context) {
    private val database = TripBudgetDatabase.create(context)
    val repository = TripRepository(database)
    val themePreferences = ThemePreferences(context)
}
