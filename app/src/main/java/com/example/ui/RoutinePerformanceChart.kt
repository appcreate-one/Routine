package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyRoutine
import java.util.Locale
import kotlin.math.roundToInt

data class DayCompletionPoint(
    val day: Int,
    val completedCount: Int,
    val totalCount: Int,
    val percentage: Float // 0f..100f
)

@Composable
fun RoutinePerformanceChartSection(
    routines: List<DailyRoutine>,
    selectedDay: Int,
    todayDay: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDetailedScrollMode by rememberSaveable { mutableStateOf(false) }
    var showMovingAverage by rememberSaveable { mutableStateOf(true) }

    val totalTasks = routines.size.coerceAtLeast(21)

    val dayPoints: List<DayCompletionPoint> = remember(routines, totalTasks) {
        (1..31).map { day ->
            val completed = routines.count { it.isDayCompleted(day) }
            val pct = (completed.toFloat() / totalTasks.toFloat()) * 100f
            DayCompletionPoint(
                day = day,
                completedCount = completed,
                totalCount = totalTasks,
                percentage = pct.coerceIn(0f, 100f)
            )
        }
    }

    // Compute performance trend metrics across elapsed/active days
    val activePoints = remember(dayPoints, todayDay) {
        val elapsed = dayPoints.filter { it.day <= todayDay || it.completedCount > 0 }
        elapsed.ifEmpty { dayPoints }
    }

    val averagePct = remember(activePoints) {
        if (activePoints.isEmpty()) 0 else activePoints.map { it.percentage }.average().roundToInt()
    }

    val perfectDaysCount = remember(dayPoints) {
        dayPoints.count { it.completedCount >= totalTasks && totalTasks > 0 }
    }

    val peakDayPoint = remember(dayPoints) {
        dayPoints.maxByOrNull { it.completedCount } ?: dayPoints.first()
    }

    val selectedPoint = remember(dayPoints, selectedDay) {
        dayPoints.getOrElse(selectedDay - 1) {
            DayCompletionPoint(selectedDay, 0, totalTasks, 0f)
        }
    }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "barChartAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("routine_performance_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.chart_section_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.chart_section_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Summary Trend KPI Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrendMiniBadge(
                    label = "AVG COMPLETION",
                    value = "$averagePct%",
                    accent = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                TrendMiniBadge(
                    label = "PEAK DAY",
                    value = "D${String.format(Locale.US, "%02d", peakDayPoint.day)} (${peakDayPoint.percentage.roundToInt()}%)",
                    accent = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                TrendMiniBadge(
                    label = "100% DAYS",
                    value = "$perfectDaysCount / 31",
                    accent = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
            }

            // View Mode & Trend Line Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isDetailedScrollMode,
                        onClick = { isDetailedScrollMode = false },
                        label = { Text("31-Day Overview") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chart_mode_overview_chip")
                    )
                    FilterChip(
                        selected = isDetailedScrollMode,
                        onClick = { isDetailedScrollMode = true },
                        label = { Text("Scrollable Detail") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chart_mode_detail_chip")
                    )
                }

                FilterChip(
                    selected = showMovingAverage,
                    onClick = { showMovingAverage = !showMovingAverage },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Toggle Trend Line",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Trend") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("chart_toggle_trend_chip")
                )
            }

            // Selected Day Inspector Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(barTierColor(selectedPoint.percentage))
                        )
                        Text(
                            text = buildString {
                                append("Day ${String.format(Locale.US, "%02d", selectedPoint.day)}")
                                if (selectedPoint.day == todayDay) append(" (TODAY)")
                                append(" • ${selectedPoint.completedCount}/${selectedPoint.totalCount} Tasks Completed")
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${selectedPoint.percentage.roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = barTierColor(selectedPoint.percentage)
                    )
                }
            }

            // Custom Canvas Bar Chart (Overview vs Scrollable Detail)
            if (!isDetailedScrollMode) {
                FullMonthCanvasBarChart(
                    points = dayPoints,
                    selectedDay = selectedDay,
                    todayDay = todayDay,
                    showTrendLine = showMovingAverage,
                    animationProgress = animProgress,
                    onSelectDay = onSelectDay
                )
            } else {
                ScrollableDetailedCanvasBarChart(
                    points = dayPoints,
                    selectedDay = selectedDay,
                    todayDay = todayDay,
                    showTrendLine = showMovingAverage,
                    animationProgress = animProgress,
                    onSelectDay = onSelectDay
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDotItem(color = Color(0xFF10B981), label = "High (≥85%)")
                LegendDotItem(color = Color(0xFFF59E0B), label = "Moderate (50–84%)")
                LegendDotItem(color = Color(0xFF06B6D4), label = "Low (<50%)")
                LegendDotItem(color = Color(0xFF38BDF8), label = "3-Day Trend")
            }
        }
    }
}

@Composable
private fun FullMonthCanvasBarChart(
    points: List<DayCompletionPoint>,
    selectedDay: Int,
    todayDay: Int,
    showTrendLine: Boolean,
    animationProgress: Float,
    onSelectDay: (Int) -> Unit
) {
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val selectedHighlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Y-Axis Labels (100%, 75%, 50%, 25%, 0%)
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .fillMaxHeight()
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf("100%", "75%", "50%", "25%", "0%").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Custom Canvas drawing all 31 bars + grid lines + 85% target line + 3-day moving average
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 18.dp)
                        .testTag("routine_bar_chart_canvas")
                        .pointerInput(points) {
                            detectTapGestures { tapOffset ->
                                if (size.width > 0 && points.isNotEmpty()) {
                                    val slotWidth = size.width.toFloat() / points.size
                                    val index = (tapOffset.x / slotWidth).toInt()
                                        .coerceIn(0, points.lastIndex)
                                    onSelectDay(points[index].day)
                                }
                            }
                        }
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    val count = points.size.coerceAtLeast(1)
                    val slotWidth = chartWidth / count
                    val barWidth = (slotWidth * 0.66f).coerceAtLeast(4f)

                    // 1. Horizontal Grid Lines (0%, 25%, 50%, 75%, 100%)
                    for (step in 0..4) {
                        val y = chartHeight * (step / 4f)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 2. Dashed Target SLA Threshold Line at 85%
                    val targetY = chartHeight * (1f - 0.85f)
                    drawLine(
                        color = Color(0xFF10B981).copy(alpha = 0.55f),
                        start = Offset(0f, targetY),
                        end = Offset(chartWidth, targetY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )

                    // 3. Draw 31 Daily Completion Bars
                    points.forEachIndexed { idx, pt ->
                        val centerX = idx * slotWidth + slotWidth / 2f
                        val barLeft = centerX - barWidth / 2f
                        val normalized = (pt.percentage / 100f).coerceIn(0f, 1f) * animationProgress
                        val barHeight = (chartHeight * normalized).coerceAtLeast(3.dp.toPx())
                        val barTop = chartHeight - barHeight

                        // Highlight column background if selected
                        if (pt.day == selectedDay) {
                            drawRoundRect(
                                color = selectedHighlightColor,
                                topLeft = Offset(idx * slotWidth, 0f),
                                size = Size(slotWidth, chartHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }

                        val (topColor, bottomColor) = barTierGradient(pt.percentage)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(topColor, bottomColor),
                                startY = barTop,
                                endY = chartHeight
                            ),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Today marker dot at base
                        if (pt.day == todayDay) {
                            drawCircle(
                                color = Color(0xFFFBBF24),
                                radius = 3.5.dp.toPx(),
                                center = Offset(centerX, chartHeight + 6.dp.toPx())
                            )
                        }
                    }

                    // 4. Smooth 3-Day Moving Average Performance Trend Line Overlay
                    if (showTrendLine && points.size > 1) {
                        val trendPath = Path()
                        val trendNodes = mutableListOf<Offset>()

                        points.forEachIndexed { idx, _ ->
                            val windowStart = (idx - 1).coerceAtLeast(0)
                            val windowEnd = (idx + 1).coerceAtMost(points.lastIndex)
                            val avgPct = points.subList(windowStart, windowEnd + 1)
                                .map { it.percentage }
                                .average()
                                .toFloat()

                            val x = idx * slotWidth + slotWidth / 2f
                            val y = chartHeight - (chartHeight * (avgPct / 100f) * animationProgress)
                            trendNodes.add(Offset(x, y))
                        }

                        trendNodes.forEachIndexed { idx, pt ->
                            if (idx == 0) {
                                trendPath.moveTo(pt.x, pt.y)
                            } else {
                                val prev = trendNodes[idx - 1]
                                val controlX = (prev.x + pt.x) / 2f
                                trendPath.cubicTo(controlX, prev.y, controlX, pt.y, pt.x, pt.y)
                            }
                        }

                        drawPath(
                            path = trendPath,
                            color = Color(0xFF38BDF8),
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Highlight selected day's trend node
                        val selIndex = (selectedDay - 1).coerceIn(0, trendNodes.lastIndex)
                        val selOffset = trendNodes[selIndex]
                        drawCircle(
                            color = Color(0xFF0B192C),
                            radius = 5.dp.toPx(),
                            center = selOffset
                        )
                        drawCircle(
                            color = Color(0xFF38BDF8),
                            radius = 3.5.dp.toPx(),
                            center = selOffset
                        )
                    }
                }
            }
        }

        // X-Axis Day Tick Labels (D01, D05, D10, D15, D20, D25, D31)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(1, 5, 10, 15, 20, 25, 31).forEach { dayTick ->
                Text(
                    text = String.format(Locale.US, "D%02d", dayTick),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dayTick == selectedDay) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        labelTextColor
                    }
                )
            }
        }
    }
}

@Composable
private fun ScrollableDetailedCanvasBarChart(
    points: List<DayCompletionPoint>,
    selectedDay: Int,
    todayDay: Int,
    showTrendLine: Boolean,
    animationProgress: Float,
    onSelectDay: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val columnWidth = 42.dp
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .testTag("scrollable_detail_bar_chart")
    ) {
        val totalWidth = columnWidth * points.size
        Column(modifier = Modifier.width(totalWidth)) {
            // Percentage Labels Row
            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEach { pt ->
                    val isSelected = pt.day == selectedDay
                    Text(
                        text = "${pt.percentage.roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(columnWidth)
                            .padding(bottom = 4.dp)
                    )
                }
            }

            // Custom Canvas for Scrollable Bars + Trend Line
            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(150.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val slotWidth = chartWidth / points.size.coerceAtLeast(1)
                val barWidth = slotWidth * 0.55f

                for (step in 0..4) {
                    val y = chartHeight * (step / 4f)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                points.forEachIndexed { idx, pt ->
                    val centerX = idx * slotWidth + slotWidth / 2f
                    val barLeft = centerX - barWidth / 2f
                    val normalized = (pt.percentage / 100f).coerceIn(0f, 1f) * animationProgress
                    val barHeight = (chartHeight * normalized).coerceAtLeast(4.dp.toPx())
                    val barTop = chartHeight - barHeight

                    val (topColor, bottomColor) = barTierGradient(pt.percentage)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(topColor, bottomColor),
                            startY = barTop,
                            endY = chartHeight
                        ),
                        topLeft = Offset(barLeft, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }

                if (showTrendLine && points.size > 1) {
                    val path = Path()
                    points.forEachIndexed { idx, pt ->
                        val x = idx * slotWidth + slotWidth / 2f
                        val y = chartHeight - (chartHeight * (pt.percentage / 100f) * animationProgress)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF38BDF8),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Clickable Day Labels Row (D01..D31)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                points.forEach { pt ->
                    val isSelected = pt.day == selectedDay
                    val isToday = pt.day == todayDay
                    Box(
                        modifier = Modifier
                            .width(columnWidth)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onSelectDay(pt.day) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "D%02d", pt.day),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendMiniBadge(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegendDotItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun barTierColor(percentage: Float): Color {
    return when {
        percentage >= 85f -> Color(0xFF10B981)
        percentage >= 50f -> Color(0xFFF59E0B)
        percentage > 0f -> Color(0xFF06B6D4)
        else -> Color(0xFF475569)
    }
}

private fun barTierGradient(percentage: Float): Pair<Color, Color> {
    return when {
        percentage >= 85f -> Color(0xFF34D399) to Color(0xFF059669)
        percentage >= 50f -> Color(0xFFFBBF24) to Color(0xFFD97706)
        percentage > 0f -> Color(0xFF22D3EE) to Color(0xFF0891B2)
        else -> Color(0xFF64748B).copy(alpha = 0.35f) to Color(0xFF334155).copy(alpha = 0.35f)
    }
}
