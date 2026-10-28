package com.tripbudget.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tripbudget.app.MainActivity
import com.tripbudget.app.R

object TripNotifications {
    const val CHANNEL_ID = "chilavu_updates"
    private const val MESSAGE_CHANNEL_ID = "chilavu_messages"

    fun areEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val updates = NotificationChannel(
            CHANNEL_ID,
            "Trip updates",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Expenses, contributions, and members on shared trips"
        }
        val messages = NotificationChannel(
            MESSAGE_CHANNEL_ID,
            "Trip messages",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Messages the trip owner sends"
        }
        manager.createNotificationChannel(updates)
        manager.createNotificationChannel(messages)
    }

    fun show(
        context: Context,
        title: String,
        body: String,
        headsUp: Boolean = false,
        tag: String? = null,
    ) {
        ensureChannel(context)
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            context,
            if (headsUp) MESSAGE_CHANNEL_ID else CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_teal))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(
                if (headsUp) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT,
            )
            .setCategory(if (headsUp) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_STATUS)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (tag.isNullOrBlank()) {
            manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } else {
            manager.notify(tag, 0, notification)
        }
    }
}
