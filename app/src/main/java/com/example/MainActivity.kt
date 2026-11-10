package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.DashboardRepository
import com.example.ui.DashboardScreen
import com.example.ui.DashboardViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private val repository by lazy {
        DashboardRepository(
            sharedDataDao = database.sharedDataDao(),
            dailyRoutineDao = database.dailyRoutineDao()
        )
    }
    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize 5:00 PM daily routine notification channel & schedule if enabled
        com.example.notifications.RoutineReminderScheduler.createNotificationChannel(this)
        if (com.example.notifications.RoutineReminderScheduler.isReminderEnabled(this)) {
            com.example.notifications.RoutineReminderScheduler.scheduleDaily5PmReminder(this)
        }

        // Intercept incoming WhatsApp / Android Share Sheet intent on cold start
        if (savedInstanceState == null) {
            viewModel.handleIncomingShareIntent(intent)
        }

        setContent {
            MyApplicationTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Intercept incoming WhatsApp / Android Share Sheet intent when activity is already running (singleTop)
        viewModel.handleIncomingShareIntent(intent)
    }
}
