package com.tripbudget.app

import android.app.Application
import com.tripbudget.app.di.AppContainer

class TripBudgetApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
