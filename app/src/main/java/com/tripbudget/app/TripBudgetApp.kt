package com.tripbudget.app

import android.app.Application
import com.tripbudget.app.di.AppContainer

class TripBudgetApp : Application() {
    /** Lazily created so Application.onCreate does not block the first splash frame. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Do not touch container here — Room / DataStore init waits until first use.
    }
}
