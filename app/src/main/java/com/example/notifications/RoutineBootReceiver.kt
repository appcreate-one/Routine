package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-schedules the daily 5:00 PM routine check after a device reboot if enabled.
 */
class RoutineBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (RoutineReminderScheduler.isReminderEnabled(context)) {
                RoutineReminderScheduler.scheduleDaily5PmReminder(context)
            }
        }
    }
}
