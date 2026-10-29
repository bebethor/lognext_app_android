package com.lognext.nexterandroid.features.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lognext.nexterandroid.MainActivity
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies

class AbsenceMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.i(ABSENCE_TAG, "fcm token_rotated=true")
        AppDependencies.initialize(applicationContext)
        AppDependencies.absenceNotifications.onTokenChanged()
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["kind"] != "absence_status") return
        val prefs = getSharedPreferences(ABSENCE_PREFS, Context.MODE_PRIVATE)
        if (!AbsencePushPolicy.accepts(prefs.getBoolean("opted_in", false), prefs.getString("subscription", null), data["subscription_id"])) {
            Log.i(ABSENCE_TAG, "fcm received suppressed=ineligible")
            return
        }
        val delivery = data["delivery_id"] ?: return
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(ABSENCE_CHANNEL, getString(R.string.absence_push_title), NotificationManager.IMPORTANCE_DEFAULT))
        val notifications = NotificationManagerCompat.from(this)
        if (!notifications.areNotificationsEnabled()) { Log.i(ABSENCE_TAG, "fcm received suppressed=permission"); return }
        val recent = prefs.getStringSet("deliveries", emptySet()).orEmpty()
        if (delivery in recent) return
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("absence_subscription", data["subscription_id"])
        }
        val pending = PendingIntent.getActivity(this, delivery.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = when (data["decision"]) { "approved" -> R.string.absence_push_approved; "rejected" -> R.string.absence_push_rejected; else -> R.string.absence_push_body }
        val notification = NotificationCompat.Builder(this, ABSENCE_CHANNEL)
            .setSmallIcon(R.drawable.ic_absence_notification)
            .setContentTitle(getString(R.string.absence_push_title)).setContentText(getString(text))
            .setAutoCancel(true).setContentIntent(pending).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        try {
            notifications.notify("absence:$delivery", 0, notification)
            prefs.edit().putStringSet("deliveries", (recent.toList().takeLast(63) + delivery).toSet()).commit()
            Log.i(ABSENCE_TAG, "fcm notification_shown=true")
        } catch (_: SecurityException) { Log.w(ABSENCE_TAG, "fcm received suppressed=permission") }
    }
}
