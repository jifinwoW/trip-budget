package com.tripbudget.app.notify

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tripbudget.app.TripBudgetApp

class ChilavuMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        (application as TripBudgetApp).container.syncEngine.registerPushToken()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Chilavu"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return
        TripNotifications.show(this, title, body, headsUp = true, tag = message.data["tag"])
    }
}
