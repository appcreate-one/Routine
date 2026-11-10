package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyRoutine
import com.example.notifications.RoutineReminderScheduler
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun DailyReminderCardSection(
    routines: List<DailyRoutine>,
    todayDay: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isReminderEnabled by remember {
        mutableStateOf(RoutineReminderScheduler.isReminderEnabled(context))
    }
    var hasPermission by remember {
        mutableStateOf(RoutineReminderScheduler.hasNotificationPermission(context))
    }
    var nextRunText by remember {
        mutableStateOf(RoutineReminderScheduler.getNextScheduledTimeFormatted(context))
    }
    var pendingTestTriggerAfterPermission by remember { mutableStateOf(false) }

    val pendingTodayTasks = remember(routines, todayDay) {
        routines.filter { !it.isDayCompleted(todayDay) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            RoutineReminderScheduler.scheduleDaily5PmReminder(context)
            isReminderEnabled = true
            nextRunText = RoutineReminderScheduler.getNextScheduledTimeFormatted(context)
            if (pendingTestTriggerAfterPermission) {
                pendingTestTriggerAfterPermission = false
                coroutineScope.launch {
                    val pendingCount = RoutineReminderScheduler.evaluateAndNotifyIncompleteTasks(
                        context = context,
                        isManualTest = true
                    )
                    Toast.makeText(
                        context,
                        if (pendingCount > 0) {
                            "5:00 PM Alert sent: $pendingCount tasks pending for Day $todayDay"
                        } else {
                            "5:00 PM Check: All 21 tasks are complete for Day $todayDay!"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            pendingTestTriggerAfterPermission = false
            Toast.makeText(
                context,
                "Notification permission denied. Grant permission in settings to receive 5:00 PM alerts.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Ensure the 5:00 PM alarm is scheduled if enabled
    LaunchedEffect(Unit) {
        if (isReminderEnabled) {
            RoutineReminderScheduler.scheduleDaily5PmReminder(context)
            nextRunText = RoutineReminderScheduler.getNextScheduledTimeFormatted(context)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("daily_reminder_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isReminderEnabled && pendingTodayTasks.isNotEmpty()) {
                Color(0xFFF59E0B).copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isReminderEnabled) {
                                    Color(0xFFF59E0B).copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isReminderEnabled) {
                                Icons.Default.NotificationsActive
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = null,
                            tint = if (isReminderEnabled) {
                                Color(0xFFF59E0B)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(id = R.string.reminder_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isReminderEnabled) {
                                "Next check: $nextRunText"
                            } else {
                                "Daily 5:00 PM check is currently paused"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isReminderEnabled) {
                                Color(0xFFF59E0B)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Switch(
                    checked = isReminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                RoutineReminderScheduler.scheduleDaily5PmReminder(context)
                                isReminderEnabled = true
                                nextRunText = RoutineReminderScheduler.getNextScheduledTimeFormatted(context)
                            }
                        } else {
                            RoutineReminderScheduler.cancelDaily5PmReminder(context)
                            isReminderEnabled = false
                            nextRunText = "Paused"
                        }
                    },
                    modifier = Modifier.testTag("reminder_5pm_switch")
                )
            }

            Text(
                text = stringResource(id = R.string.reminder_card_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Live Status Strip for Today's 5:00 PM Readiness
            Surface(
                color = if (pendingTodayTasks.isEmpty()) {
                    Color(0xFF10B981).copy(alpha = 0.14f)
                } else {
                    Color(0xFFF59E0B).copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (pendingTodayTasks.isEmpty()) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.AccessTime
                        },
                        contentDescription = null,
                        tint = if (pendingTodayTasks.isEmpty()) {
                            Color(0xFF10B981)
                        } else {
                            Color(0xFFF59E0B)
                        },
                        modifier = Modifier.size(18.dp)
                    )

                    val statusText = if (pendingTodayTasks.isEmpty()) {
                        "All 21 routine checkpoints completed for Day $todayDay — 5:00 PM alert satisfied!"
                    } else {
                        val firstFew = pendingTodayTasks.take(2).joinToString(", ") {
                            "#${String.format(Locale.US, "%02d", it.id)} ${it.taskName}"
                        }
                        "${pendingTodayTasks.size} tasks still pending for Day $todayDay (e.g., $firstFew)"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Action Row: Request Permission (if needed on Android 13+) + Test 5:00 PM Check Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF0B192C)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grant_notification_permission_button")
                    ) {
                        Text("Allow Notifications")
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                            pendingTestTriggerAfterPermission = true
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            coroutineScope.launch {
                                val pendingCount =
                                    RoutineReminderScheduler.evaluateAndNotifyIncompleteTasks(
                                        context = context,
                                        isManualTest = true
                                    )
                                Toast.makeText(
                                    context,
                                    if (pendingCount > 0) {
                                        "5:00 PM Notification fired ($pendingCount tasks pending for Day $todayDay)"
                                    } else {
                                        "5:00 PM Check fired: All 21 tasks completed for Day $todayDay!"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_5pm_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(id = R.string.reminder_test_now))
                }
            }
        }
    }
}
