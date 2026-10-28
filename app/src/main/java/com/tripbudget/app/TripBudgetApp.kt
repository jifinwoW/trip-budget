package com.tripbudget.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tripbudget.app.di.AppContainer
import com.tripbudget.app.notify.TripNotifications

class TripBudgetApp : Application() {
    /** Lazily created so Application.onCreate does not block the first splash frame. */
    val container: AppContainer by lazy { AppContainer(this) }

    @Volatile
    var isInForeground: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        TripNotifications.ensureChannel(this)
        registerActivityLifecycleCallbacks(ForegroundCallbacks())
    }

    private inner class ForegroundCallbacks : ActivityLifecycleCallbacks {
        private var started = 0

        override fun onActivityStarted(activity: Activity) {
            started += 1
            isInForeground = started > 0
        }

        override fun onActivityStopped(activity: Activity) {
            started -= 1
            isInForeground = started > 0
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
