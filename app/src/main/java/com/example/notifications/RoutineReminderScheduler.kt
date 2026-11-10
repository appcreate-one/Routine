package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.RoutineCatalog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

object RoutineReminderScheduler {

    const val CHANNEL_ID = "branch_routine_5pm_channel"
    private const val CHANNEL_NAME = "5:00 PM Daily Routine Reminders"
    private const val NOTIFICATION_ID = 1700
    private const val ALARM_REQUEST_CODE = 17005
    private const val PREFS_NAME = "branch_ops_reminder_prefs"
    private const val KEY_REMINDER_ENABLED = "reminder_5pm_enabled"
    private const val KEY_NEXT_SCHEDULED_MILLIS = "next_scheduled_5pm_millis"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily 5:00 PM alerts when branch routine tasks are incomplete"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun isReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REMINDER_ENABLED, true)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Schedules the next daily 5:00 PM (17:00) routine check using AlarmManager.setWindow
     * (compliant with Google Play policy without requiring SCHEDULE_EXACT_ALARM).
     */
    fun scheduleDaily5PmReminder(context: Context, forceNextDay: Boolean = false): Long {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context)

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (forceNextDay || timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerAtMillis = target.timeInMillis
        val windowLengthMillis = 10 * 60 * 1000L // 10-minute delivery window at 5:00 PM

        alarmManager?.setWindow(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            windowLengthMillis,
            pendingIntent
        )

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REMINDER_ENABLED, true)
            .putLong(KEY_NEXT_SCHEDULED_MILLIS, triggerAtMillis)
            .apply()

        return triggerAtMillis
    }

    fun cancelDaily5PmReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context)
        alarmManager?.cancel(pendingIntent)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REMINDER_ENABLED, false)
            .putLong(KEY_NEXT_SCHEDULED_MILLIS, 0L)
            .apply()
    }

    fun getNextScheduledTimeFormatted(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_REMINDER_ENABLED, true)
        if (!enabled) return "Paused"

        val storedMillis = prefs.getLong(KEY_NEXT_SCHEDULED_MILLIS, 0L)
        val effectiveMillis = if (storedMillis > System.currentTimeMillis()) {
            storedMillis
        } else {
            scheduleDaily5PmReminder(context)
        }
        return SimpleDateFormat("EEE, dd MMM 'at' hh:mm a", Locale.getDefault())
            .format(Date(effectiveMillis))
    }

    /**
     * Queries Room Database for today's routine completion and fires a notification
     * if any of the 21 tasks have not been marked off yet.
     *
     * @return Number of incomplete tasks for today.
     */
    suspend fun evaluateAndNotifyIncompleteTasks(
        context: Context,
        isManualTest: Boolean = false
    ): Int {
        createNotificationChannel(context)
        val dao = AppDatabase.getInstance(context).dailyRoutineDao()
        val storedRoutines = dao.getAllRoutines().first()
        val routines = if (storedRoutines.isEmpty()) {
            RoutineCatalog.buildInitialEntities()
        } else {
            storedRoutines
        }

        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, 31)
        val incompleteTasks = routines.filter { !it.isDayCompleted(today) }

        if (incompleteTasks.isNotEmpty() && hasNotificationPermission(context)) {
            val previewList = incompleteTasks.take(4).joinToString("\n") {
                "• #${String.format(Locale.US, "%02d", it.id)} ${it.taskName}"
            }
            val remainingMore = (incompleteTasks.size - 4).coerceAtLeast(0)
            val bigBody = buildString {
                append("${incompleteTasks.size} of ${routines.size} daily branch tasks are still pending for Day $today:\n")
                append(previewList)
                if (remainingMore > 0) {
                    append("\n+$remainingMore more pending before 6:00 PM closing.")
                }
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(
                    "5:00 PM Routine Check: ${incompleteTasks.size} Tasks Pending"
                )
                .setContentText(
                    "Complete ${incompleteTasks.size} remaining tasks before 6:00 PM system closing."
                )
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Handled gracefully if permission was revoked
            }
        } else if (isManualTest && incompleteTasks.isEmpty() && hasNotificationPermission(context)) {
            // If user taps "Test 5:00 PM Check" when all 21 tasks are already done, show confirmation notification
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("5:00 PM Routine Check: All 21 Tasks Complete!")
                .setContentText("Great job! All 21 branch routine checkpoints are marked off for Day $today.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
            }
        }

        return incompleteTasks.size
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, RoutineReminderReceiver::class.java).apply {
            action = ACTION_CHECK_5PM_ROUTINE
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    const val ACTION_CHECK_5PM_ROUTINE = "com.example.action.CHECK_5PM_ROUTINE"
}
