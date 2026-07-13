package com.lognext.nexterandroid.features.more

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lognext.nexterandroid.R
import java.util.Calendar

object ClockNotificationScheduler {
    private const val PreferencesName = "clock_notification_settings"
    private const val ChannelId = "nexter_clock_notifications"
    private const val ActionNotify = "com.lognext.nexterandroid.CLOCK_NOTIFICATION"
    private const val ActionBoot = "android.intent.action.BOOT_COMPLETED"
    private const val ExtraId = "id"
    private const val ExtraTitle = "title"
    private const val ExtraBody = "body"
    private const val ExtraRepeatWeekly = "repeatWeekly"

    fun loadSettings(context: Context, viewModel: MoreViewModel) {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        viewModel.notificationSettings.forEach { setting ->
            viewModel.notificationEnabled[setting.id] = preferences.getBoolean(enabledKey(setting.id), false)
            viewModel.notificationMinutes[setting.id] = preferences.getInt(minutesKey(setting.id), setting.defaultMinutes)
        }
    }

    fun reschedule(
        context: Context,
        viewModel: MoreViewModel,
        longOpenTriggerAtMillis: Long? = null,
        skipNoRecordToday: Boolean = false
    ) {
        saveSettings(context, viewModel)
        ensureChannel(context)
        cancelAll(context)
        if (viewModel.notificationEnabled.none { it.value }) return

        scheduleWorkdayReminder(
            context = context,
            idPrefix = "clock.work.start",
            enabled = viewModel.notificationEnabled["workStart"] == true,
            minutes = viewModel.notificationMinutes["workStart"] ?: 9 * 60,
            title = context.getString(R.string.clock_notification_work_start_push_title),
            body = context.getString(R.string.clock_notification_work_start_push_body)
        )
        scheduleWorkdayReminder(
            context = context,
            idPrefix = "clock.lunch.start",
            enabled = viewModel.notificationEnabled["lunchStart"] == true,
            minutes = viewModel.notificationMinutes["lunchStart"] ?: 14 * 60,
            title = context.getString(R.string.clock_notification_lunch_start_push_title),
            body = context.getString(R.string.clock_notification_lunch_start_push_body)
        )
        scheduleWorkdayReminder(
            context = context,
            idPrefix = "clock.lunch.end",
            enabled = viewModel.notificationEnabled["lunchEnd"] == true,
            minutes = viewModel.notificationMinutes["lunchEnd"] ?: 15 * 60,
            title = context.getString(R.string.clock_notification_lunch_end_push_title),
            body = context.getString(R.string.clock_notification_lunch_end_push_body)
        )
        scheduleWorkdayReminder(
            context = context,
            idPrefix = "clock.work.end",
            enabled = viewModel.notificationEnabled["workEnd"] == true,
            minutes = viewModel.notificationMinutes["workEnd"] ?: 18 * 60,
            title = context.getString(R.string.clock_notification_work_end_push_title),
            body = context.getString(R.string.clock_notification_work_end_push_body)
        )
        if (viewModel.notificationEnabled["noRecordEndOfDay"] == true) {
            scheduleOneTime(
                context = context,
                id = "clock.no.record.end.day",
                triggerAtMillis = nextWorkdayAt(19, 0, skipToday = skipNoRecordToday),
                title = context.getString(R.string.clock_notification_no_record_push_title),
                body = context.getString(R.string.clock_notification_no_record_push_body),
                repeatWeekly = false
            )
        }
        if (viewModel.notificationEnabled["longOpenEntry"] == true && longOpenTriggerAtMillis != null && longOpenTriggerAtMillis > System.currentTimeMillis()) {
            scheduleOneTime(
                context = context,
                id = "clock.long.open.entry",
                triggerAtMillis = longOpenTriggerAtMillis,
                title = context.getString(R.string.clock_notification_long_open_push_title),
                body = context.getString(R.string.clock_notification_long_open_push_body),
                repeatWeekly = false
            )
        }
    }

    fun rescheduleFromPreferences(context: Context) {
        val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        ensureChannel(context)
        cancelAll(context)
        val settings = mapOf(
            "workStart" to Triple("clock.work.start", 9 * 60, context.getString(R.string.clock_notification_work_start_push_body)),
            "lunchStart" to Triple("clock.lunch.start", 14 * 60, context.getString(R.string.clock_notification_lunch_start_push_body)),
            "lunchEnd" to Triple("clock.lunch.end", 15 * 60, context.getString(R.string.clock_notification_lunch_end_push_body)),
            "workEnd" to Triple("clock.work.end", 18 * 60, context.getString(R.string.clock_notification_work_end_push_body))
        )
        settings.forEach { (id, data) ->
            val (prefix, defaultMinutes, body) = data
            scheduleWorkdayReminder(
                context = context,
                idPrefix = prefix,
                enabled = preferences.getBoolean(enabledKey(id), false),
                minutes = preferences.getInt(minutesKey(id), defaultMinutes),
                title = when (id) {
                    "lunchStart" -> context.getString(R.string.clock_notification_lunch_start_push_title)
                    "lunchEnd" -> context.getString(R.string.clock_notification_lunch_end_push_title)
                    "workEnd" -> context.getString(R.string.clock_notification_work_end_push_title)
                    else -> context.getString(R.string.clock_notification_work_start_push_title)
                },
                body = body
            )
        }
        if (preferences.getBoolean(enabledKey("noRecordEndOfDay"), false)) {
            scheduleOneTime(
                context = context,
                id = "clock.no.record.end.day",
                triggerAtMillis = nextWorkdayAt(19, 0, skipToday = false),
                title = context.getString(R.string.clock_notification_no_record_push_title),
                body = context.getString(R.string.clock_notification_no_record_push_body),
                repeatWeekly = false
            )
        }
    }

    fun showNotification(context: Context, id: String, title: String, body: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
    }

    fun scheduleNextWeek(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ExtraId) ?: return
        val title = intent.getStringExtra(ExtraTitle).orEmpty()
        val body = intent.getStringExtra(ExtraBody).orEmpty()
        val next = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
        scheduleOneTime(context, id, next, title, body, repeatWeekly = true)
    }

    private fun saveSettings(context: Context, viewModel: MoreViewModel) {
        val editor = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).edit()
        viewModel.notificationSettings.forEach { setting ->
            editor.putBoolean(enabledKey(setting.id), viewModel.notificationEnabled[setting.id] == true)
            editor.putInt(minutesKey(setting.id), viewModel.notificationMinutes[setting.id] ?: setting.defaultMinutes)
        }
        editor.apply()
    }

    private fun scheduleWorkdayReminder(
        context: Context,
        idPrefix: String,
        enabled: Boolean,
        minutes: Int,
        title: String,
        body: String
    ) {
        if (!enabled) return
        val hour = (minutes / 60).coerceIn(0, 23)
        val minute = (minutes % 60).coerceIn(0, 59)
        for (weekday in Calendar.MONDAY..Calendar.FRIDAY) {
            scheduleOneTime(
                context = context,
                id = "$idPrefix.weekday.$weekday",
                triggerAtMillis = nextWeekdayAt(weekday, hour, minute),
                title = title,
                body = body,
                repeatWeekly = true
            )
        }
    }

    private fun scheduleOneTime(
        context: Context,
        id: String,
        triggerAtMillis: Long,
        title: String,
        body: String,
        repeatWeekly: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, id, title, body, repeatWeekly)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun pendingIntent(context: Context, id: String, title: String, body: String, repeatWeekly: Boolean): PendingIntent {
        val intent = Intent(context, ClockNotificationReceiver::class.java).apply {
            action = ActionNotify
            putExtra(ExtraId, id)
            putExtra(ExtraTitle, title)
            putExtra(ExtraBody, body)
            putExtra(ExtraRepeatWeekly, repeatWeekly)
        }
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(
            "clock.work.start",
            "clock.lunch.start",
            "clock.lunch.end",
            "clock.work.end"
        ).forEach { prefix ->
            for (weekday in Calendar.MONDAY..Calendar.FRIDAY) {
                alarmManager.cancel(pendingIntent(context, "$prefix.weekday.$weekday", "", "", false))
            }
        }
        alarmManager.cancel(pendingIntent(context, "clock.no.record.end.day", "", "", false))
        alarmManager.cancel(pendingIntent(context, "clock.long.open.entry", "", "", false))
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ChannelId,
            context.getString(R.string.more_clock_notifications),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun nextWeekdayAt(weekday: Int, hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, weekday)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!candidate.after(now)) candidate.add(Calendar.DAY_OF_YEAR, 7)
        return candidate.timeInMillis
    }

    private fun nextWorkdayAt(hour: Int, minute: Int, skipToday: Boolean): Long {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (skipToday || !after(now)) add(Calendar.DAY_OF_YEAR, 1)
            while (get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return candidate.timeInMillis
    }

    private fun enabledKey(id: String) = "enabled_$id"
    private fun minutesKey(id: String) = "minutes_$id"
}

class ClockNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            ClockNotificationScheduler.rescheduleFromPreferences(context)
            return
        }
        val id = intent.getStringExtra("id") ?: return
        val title = intent.getStringExtra("title").orEmpty()
        val body = intent.getStringExtra("body").orEmpty()
        ClockNotificationScheduler.showNotification(context, id, title, body)
        if (intent.getBooleanExtra("repeatWeekly", false)) {
            ClockNotificationScheduler.scheduleNextWeek(context, intent)
        }
    }
}
