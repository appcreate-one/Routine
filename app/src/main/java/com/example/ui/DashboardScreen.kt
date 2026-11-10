package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DailyRoutine
import com.example.data.SharedData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val STICKY_TASK_COLUMN_WIDTH = 228.dp
private val DAY_CELL_WIDTH = 48.dp
private val ROW_HEIGHT = 60.dp

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val horizontalGridScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current

    var showAddShareDialog by rememberSaveable { mutableStateOf(false) }
    var showResetConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val todayCompletedCount = remember(uiState.routines, uiState.selectedDayOfMonth) {
        uiState.routines.count { it.isDayCompleted(uiState.selectedDayOfMonth) }
    }
    val totalRoutineTasks = uiState.routines.size.coerceAtLeast(21)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("dashboard_lazy_column"),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Header Banner
            item {
                HeroHeaderSection(
                    selectedDay = uiState.selectedDayOfMonth,
                    todayCompleted = todayCompletedCount,
                    totalTasks = totalRoutineTasks,
                    onSimulateShare = { viewModel.simulateIncomingWhatsAppShare() },
                    onOpenManualShareDialog = { showAddShareDialog = true }
                )
            }

            // 2. Live Incoming Share Notification Banner
            item {
                AnimatedVisibility(
                    visible = uiState.lastCapturedShareBanner != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.lastCapturedShareBanner?.let { message ->
                        IncomingShareToastBanner(
                            message = message,
                            onDismiss = { viewModel.dismissBanner() }
                        )
                    }
                }
            }

            // 3. SECTION A: WhatsApp Stats & Recent Shares
            item {
                WhatsAppStatsSection(
                    uiState = uiState,
                    todayCompletedCount = todayCompletedCount,
                    totalTasks = totalRoutineTasks,
                    onSelectFilter = { viewModel.setShareFilter(it) },
                    onSimulateShare = { viewModel.simulateIncomingWhatsAppShare() },
                    onClearAllShares = { viewModel.clearAllShares() },
                    onDeleteShare = { viewModel.deleteShare(it) },
                    onCopyShare = { content ->
                        copyToClipboard(context, content)
                    }
                )
            }

            // 4. 5:00 PM Daily Routine Notification Scheduler & Readiness Card
            item {
                DailyReminderCardSection(
                    routines = uiState.routines,
                    todayDay = uiState.todayDayOfMonth
                )
            }

            // 5. Monthly Performance Trend Bar Chart (Custom Canvas 31-Day Completion Visualization)
            item {
                RoutinePerformanceChartSection(
                    routines = uiState.routines,
                    selectedDay = uiState.selectedDayOfMonth,
                    todayDay = uiState.todayDayOfMonth,
                    onSelectDay = { day ->
                        viewModel.selectDayOfMonth(day)
                        coroutineScope.launch {
                            val targetPx = with(density) {
                                ((day - 1).coerceAtLeast(0) * DAY_CELL_WIDTH.toPx()).toInt()
                            }
                            horizontalGridScrollState.animateScrollTo(targetPx)
                        }
                    }
                )
            }

            // 5. SECTION B: 31-Day Daily Routine Grid Header & Controls
            item {
                RoutineControlPanel(
                    uiState = uiState,
                    todayCompletedCount = todayCompletedCount,
                    totalTasks = totalRoutineTasks,
                    onSearchChange = { viewModel.setRoutineSearchQuery(it) },
                    onPhaseChange = { viewModel.setPhaseFilter(it) },
                    onFocusDay = { day ->
                        viewModel.selectDayOfMonth(day)
                        coroutineScope.launch {
                            val targetPx = with(density) {
                                ((day - 1).coerceAtLeast(0) * DAY_CELL_WIDTH.toPx()).toInt()
                            }
                            horizontalGridScrollState.animateScrollTo(targetPx)
                        }
                    },
                    onMarkSelectedDayComplete = {
                        viewModel.markAllTasksForDay(uiState.selectedDayOfMonth, true)
                    },
                    onRequestResetMonth = { showResetConfirmDialog = true }
                )
            }

            // 5. Sticky 31-Day Column Header + 21 Routine Tasks Grid Card
            item {
                DailyRoutineStickyGridCard(
                    routines = uiState.filteredRoutines,
                    allRoutines = uiState.routines,
                    selectedDay = uiState.selectedDayOfMonth,
                    todayDay = uiState.todayDayOfMonth,
                    horizontalScrollState = horizontalGridScrollState,
                    onSelectDay = { viewModel.selectDayOfMonth(it) },
                    onToggleCell = { taskId, day ->
                        viewModel.toggleRoutineTaskDay(taskId, day)
                    }
                )
            }
        }
    }

    if (showAddShareDialog) {
        ManualShareEntryDialog(
            onDismiss = { showAddShareDialog = false },
            onSaveShare = { type, content, title ->
                viewModel.addManualOrSimulatedShare(type, content, title)
                showAddShareDialog = false
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset 31-Day Routine Grid?") },
            text = {
                Text(
                    "This will clear all completion checkmarks across all 31 days for the 21 daily branch routine tasks."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllRoutines()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text("Reset Month")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroHeaderSection(
    selectedDay: Int,
    todayCompleted: Int,
    totalTasks: Int,
    onSimulateShare: () -> Unit,
    onOpenManualShareDialog: () -> Unit
) {
    val progress = if (totalTasks > 0) todayCompleted.toFloat() / totalTasks.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_dashboard_1794297973063),
                contentDescription = "Branch Operations Command Center Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC0B192C),
                                Color(0xF20B192C)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFF34D399))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "WHATSAPP SHARE READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF34D399)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "DAY $selectedDay • $todayCompleted/$totalTasks DONE",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.dashboard_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )

                Text(
                    text = "Direct WhatsApp Share Receiver (ACTION_SEND) & 21-Point Daily Branch Operations Grid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color(0xFF10B981),
                    trackColor = Color.White.copy(alpha = 0.16f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSimulateShare,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF0B192C)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_whatsapp_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "Simulate WhatsApp Share",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.simulate_whatsapp_share),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenManualShareDialog,
                        border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("log_manual_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log Share Manually",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.add_manual_share),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingShareToastBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Dismiss notification",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun WhatsAppStatsSection(
    uiState: DashboardUiState,
    todayCompletedCount: Int,
    totalTasks: Int,
    onSelectFilter: (String) -> Unit,
    onSimulateShare: () -> Unit,
    onClearAllShares: () -> Unit,
    onDeleteShare: (SharedData) -> Unit,
    onCopyShare: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.section_whatsapp_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Real-time Room DB metrics from android.intent.action.SEND",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uiState.sharedItems.isNotEmpty()) {
                TextButton(
                    onClick = onClearAllShares,
                    modifier = Modifier.testTag("clear_all_shares_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Shares",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.clear_shares))
                }
            }
        }

        // 4 KPI Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = stringResource(id = R.string.stat_total_shares),
                value = uiState.stats.totalShares.toString(),
                subtitle = "${uiState.stats.textShares} text notes",
                accentColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Share,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_total_shares")
            )
            StatMetricCard(
                title = stringResource(id = R.string.stat_links_shared),
                value = uiState.stats.linkShares.toString(),
                subtitle = "URLs & Sheets",
                accentColor = MaterialTheme.colorScheme.tertiary,
                icon = Icons.Default.Link,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_links")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = stringResource(id = R.string.stat_media_shared),
                value = uiState.stats.mediaShares.toString(),
                subtitle = "Photos & PDFs",
                accentColor = MaterialTheme.colorScheme.secondary,
                icon = Icons.Default.Image,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_media")
            )
            val pct = if (totalTasks > 0) (todayCompletedCount * 100) / totalTasks else 0
            StatMetricCard(
                title = stringResource(id = R.string.stat_today_routine),
                value = "$pct%",
                subtitle = "$todayCompletedCount of $totalTasks tasks",
                accentColor = Color(0xFF10B981),
                icon = Icons.Default.DoneAll,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stat_card_routine_pct")
            )
        }

        // Filter Chips for Recent WhatsApp Shares
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                "ALL" to "All Shares (${uiState.stats.totalShares})",
                "TEXT" to "Text (${uiState.stats.textShares})",
                "LINK" to "Links (${uiState.stats.linkShares})",
                "MEDIA" to "Media (${uiState.stats.mediaShares})"
            )
            items(filters) { (key, label) ->
                FilterChip(
                    selected = uiState.selectedShareFilter == key,
                    onClick = { onSelectFilter(key) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("share_filter_$key")
                )
            }
        }

        // Recent Shares Feed Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.recent_shares_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (uiState.filteredSharedItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.empty_shares_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(id = R.string.empty_shares_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = onSimulateShare,
                            modifier = Modifier.testTag("empty_state_simulate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load Sample WhatsApp Share")
                        }
                    }
                } else {
                    uiState.filteredSharedItems.take(6).forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                        SharedDataRowItem(
                            item = item,
                            onCopy = { onCopyShare(item.rawContent) },
                            onDelete = { onDeleteShare(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun SharedDataRowItem(
    item: SharedData,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, badgeColor) = when (item.contentType) {
        "LINK" -> Icons.Default.Link to Color(0xFF06B6D4)
        "IMAGE" -> Icons.Default.Image to Color(0xFFF59E0B)
        "VIDEO" -> Icons.Default.VideoFile to Color(0xFFF43F5E)
        "DOCUMENT", "AUDIO" -> Icons.Default.Description to Color(0xFFA855F7)
        else -> Icons.Default.Sms to Color(0xFF10B981)
    }

    val formattedTime = remember(item.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("shared_item_${item.id}"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(badgeColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.contentType,
                tint = badgeColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.contentType,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.titleOrSubject.isNotBlank()) {
                Text(
                    text = item.titleOrSubject,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = item.rawContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy content",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete shared item",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RoutineControlPanel(
    uiState: DashboardUiState,
    todayCompletedCount: Int,
    totalTasks: Int,
    onSearchChange: (String) -> Unit,
    onPhaseChange: (String) -> Unit,
    onFocusDay: (Int) -> Unit,
    onMarkSelectedDayComplete: () -> Unit,
    onRequestResetMonth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.section_routine_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sticky task column + 31 horizontal day columns (Day ${uiState.selectedDayOfMonth}: $todayCompletedCount/$totalTasks completed)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Field for 21 Tasks
        OutlinedTextField(
            value = uiState.routineSearchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("routine_search_input"),
            placeholder = { Text(stringResource(id = R.string.search_tasks_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search tasks"
                )
            },
            trailingIcon = {
                if (uiState.routineSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        // Phase Filter Chips
        val phases = listOf(
            "All Phases",
            "Morning Opening",
            "Core Ops & HM",
            "Afternoon Closing",
            "EOD Verification"
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(phases) { phase ->
                FilterChip(
                    selected = uiState.selectedPhaseFilter == phase,
                    onClick = { onPhaseChange(phase) },
                    label = { Text(phase) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        // Quick Actions Row (Focus Today, Complete Day, Reset)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onFocusDay(uiState.todayDayOfMonth) },
                modifier = Modifier.testTag("focus_today_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scroll to Today (D${uiState.todayDayOfMonth})")
            }

            Button(
                onClick = onMarkSelectedDayComplete,
                modifier = Modifier
                    .weight(1f)
                    .testTag("complete_day_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Complete Day ${uiState.selectedDayOfMonth}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRequestResetMonth,
                modifier = Modifier.testTag("reset_month_icon_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.reset_month_confirm),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Sticky-header 31-day routine tracker grid:
 * - Left column (STICKY_TASK_COLUMN_WIDTH) stays fixed while scrolling horizontally through Days 1..31.
 * - Top header row displays the 31 day columns with completion count and Today indicator.
 */
@Composable
private fun DailyRoutineStickyGridCard(
    routines: List<DailyRoutine>,
    allRoutines: List<DailyRoutine>,
    selectedDay: Int,
    todayDay: Int,
    horizontalScrollState: ScrollState,
    onSelectDay: (Int) -> Unit,
    onToggleCell: (taskId: Int, day: Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("daily_routine_grid_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sticky Header Row (Fixed Top-Left Task Label + Horizontally Scrollable 31 Day Headers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Top-left sticky corner cell
                Box(
                    modifier = Modifier
                        .width(STICKY_TASK_COLUMN_WIDTH)
                        .height(64.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "TASK CATEGORY (1-21)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Sticky column • Scroll 31 days →",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Horizontally scrollable 1..31 Day Headers
                Row(
                    modifier = Modifier.horizontalScroll(horizontalScrollState)
                ) {
                    for (day in 1..31) {
                        val isSelected = day == selectedDay
                        val isToday = day == todayDay
                        val completedOnDay = allRoutines.count { it.isDayCompleted(day) }
                        val dayBg = when {
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                            else -> Color.Transparent
                        }

                        Column(
                            modifier = Modifier
                                .width(DAY_CELL_WIDTH)
                                .height(64.dp)
                                .background(dayBg)
                                .clickable { onSelectDay(day) }
                                .padding(vertical = 6.dp)
                                .testTag("day_header_$day"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isToday) "TODAY" else "DAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = String.format(Locale.US, "%02d", day),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = "$completedOnDay/21",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (completedOnDay == 21) {
                                    Color(0xFF10B981)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            // 21 Routine Task Rows
            routines.forEachIndexed { index, routine ->
                val rowBg = if (index % 2 == 0) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .background(rowBg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sticky Left Column: Task ID, Exact Name, Phase & Monthly Counter
                    Row(
                        modifier = Modifier
                            .width(STICKY_TASK_COLUMN_WIDTH)
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d", routine.id),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = routine.taskName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${routine.phaseTag} • ${routine.completedCount()}/31d",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Subtle vertical divider separating sticky column from 31 scrollable day cells
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )

                    // Scrollable 31 Day Check Cells synchronized via horizontalScrollState
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .horizontalScroll(horizontalScrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val completedSet = remember(routine.completedDaysCsv) {
                            routine.completedDaysSet()
                        }
                        for (day in 1..31) {
                            val isChecked = day in completedSet
                            val isSelectedDay = day == selectedDay
                            val cellHighlight = if (isSelectedDay) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            } else {
                                Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .width(DAY_CELL_WIDTH)
                                    .fillMaxHeight()
                                    .background(cellHighlight)
                                    .clickable { onToggleCell(routine.id, day) }
                                    .testTag("cell_task_${routine.id}_day_$day"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(
                                            if (isChecked) CircleShape else RoundedCornerShape(8.dp)
                                        )
                                        .background(
                                            if (isChecked) {
                                                Color(0xFF10B981)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isChecked) {
                                                Color(0xFF10B981)
                                            } else if (isSelectedDay) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            },
                                            shape = if (isChecked) CircleShape else RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecked) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed task ${routine.id} on day $day",
                                            tint = Color(0xFF0B192C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (index < routines.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualShareEntryDialog(
    onDismiss: () -> Unit,
    onSaveShare: (contentType: String, rawContent: String, title: String) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf("TEXT") }
    var titleInput by rememberSaveable { mutableStateOf("") }
    var rawContentInput by rememberSaveable { mutableStateOf("") }

    val types = listOf("TEXT", "LINK", "IMAGE", "DOCUMENT")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log WhatsApp Share Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select content type and paste shared WhatsApp text, URL, or media URI:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(types) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) }
                        )
                    }
                }
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Summary / Sender Label") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_share_title_input")
                )
                OutlinedTextField(
                    value = rawContentInput,
                    onValueChange = { rawContentInput = it },
                    label = { Text("Shared Text / URL / Media URI") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_share_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawContentInput.isNotBlank()) {
                        onSaveShare(selectedType, rawContentInput, titleInput)
                    }
                },
                enabled = rawContentInput.isNotBlank(),
                modifier = Modifier.testTag("dialog_save_share_button")
            ) {
                Text("Save to Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("WhatsApp Share", text))
    Toast.makeText(context, "Copied shared content to clipboard", Toast.LENGTH_SHORT).show()
}
