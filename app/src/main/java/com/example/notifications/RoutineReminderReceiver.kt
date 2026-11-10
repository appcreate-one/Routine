package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Triggered daily at 5:00 PM by AlarmManager to inspect Room Database
 * and notify the user if any of today's 21 routine tasks are still incomplete.
 */
class RoutineReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (!RoutineReminderScheduler.isReminderEnabled(context)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RoutineReminderScheduler.evaluateAndNotifyIncompleteTasks(
                    context = context.applicationContext,
                    isManualTest = false
                )
                // Schedule the next day's 5:00 PM check
                RoutineReminderScheduler.scheduleDaily5PmReminder(
                    context = context.applicationContext,
                    forceNextDay = true
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
