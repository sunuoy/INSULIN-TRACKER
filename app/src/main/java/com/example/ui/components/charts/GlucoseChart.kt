package com.example.ui.components.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlucoseReading
import com.example.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clinical Time-in-Range (TIR) Segmented Progress Bar & Metric Pills
 * Standard Clinical Targets:
 *  - Low (< 70 mg/dL): < 4%
 *  - In Range (70 - 180 mg/dL): > 70% (ADA Gold Standard)
 *  - High (> 180 mg/dL): < 25%
 */
@Composable
fun TimeInRangeCard(
    glucoseList: List<GlucoseReading>,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val targetMin = profile.targetGlucoseMin
    val targetMax = profile.targetGlucoseMax
    val isMmol = profile.glucoseUnit == "mmol/L"

    val (lowCount, inRangeCount, highCount, total) = remember(glucoseList, targetMin, targetMax) {
        if (glucoseList.isEmpty()) {
            listOf(0, 0, 0, 0)
        } else {
            var low = 0
            var inRange = 0
            var high = 0
            glucoseList.forEach { reading ->
                when {
                    reading.readingValue < targetMin -> low++
                    reading.readingValue > targetMax -> high++
                    else -> inRange++
                }
            }
            listOf(low, inRange, high, glucoseList.size)
        }
    }

    val lowPct = if (total > 0) (lowCount.toFloat() / total * 100f) else 0f
    val inRangePct = if (total > 0) (inRangeCount.toFloat() / total * 100f) else 100f
    val highPct = if (total > 0) (highCount.toFloat() / total * 100f) else 0f

    // Animated progress transitions
    val animatedLow by animateFloatAsState(targetValue = lowPct / 100f, animationSpec = tween(900), label = "lowPct")
    val animatedInRange by animateFloatAsState(targetValue = inRangePct / 100f, animationSpec = tween(900), label = "inRangePct")
    val animatedHigh by animateFloatAsState(targetValue = highPct / 100f, animationSpec = tween(900), label = "highPct")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Time in Range (TIR)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target Range: ${targetMin.toInt()}–${targetMax.toInt()} ${profile.glucoseUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // In-Range Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (inRangePct >= 70f) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${inRangePct.toInt()}% In Range",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (inRangePct >= 70f) Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment Segmented Bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
            ) {
                val w = size.width
                val h = size.height

                if (total == 0) {
                    drawRoundRect(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        cornerRadius = CornerRadius(h / 2, h / 2),
                        size = size
                    )
                } else {
                    var currentX = 0f

                    // 1. Low segment (Red)
                    val lowW = w * animatedLow
                    if (lowW > 0) {
                        drawRect(
                            color = Color(0xFFEF4444),
                            topLeft = Offset(currentX, 0f),
                            size = Size(lowW, h)
                        )
                        currentX += lowW
                    }

                    // 2. In-range segment (Emerald Green)
                    val inRangeW = w * animatedInRange
                    if (inRangeW > 0) {
                        drawRect(
                            color = Color(0xFF10B981),
                            topLeft = Offset(currentX, 0f),
                            size = Size(inRangeW, h)
                        )
                        currentX += inRangeW
                    }

                    // 3. High segment (Amber/Orange)
                    val highW = w * animatedHigh
                    if (highW > 0) {
                        drawRect(
                            color = Color(0xFFF59E0B),
                            topLeft = Offset(currentX, 0f),
                            size = Size(highW, h)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Column Metric Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Low Pill
                TirMetricPill(
                    label = "Low (<${targetMin.toInt()})",
                    percent = "${lowPct.toInt()}%",
                    count = "$lowCount readings",
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // In Target Pill
                TirMetricPill(
                    label = "In Target",
                    percent = "${inRangePct.toInt()}%",
                    count = "$inRangeCount readings",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // High Pill
                TirMetricPill(
                    label = "High (>${targetMax.toInt()})",
                    percent = "${highPct.toInt()}%",
                    count = "$highCount readings",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TirMetricPill(
    label: String,
    percent: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                Text(
                    text = percent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * Interactive Glucose Trend Sparkline & Curve Chart
 * Features:
 *  - Cubic Bezier curve smoothing
 *  - Gradient under-fill
 *  - Target Range (70-180) shaded safe-zone band
 *  - Touch interactive tooltip to inspect historical points
 */
@Composable
fun InteractiveGlucoseTrendChart(
    readings: List<GlucoseReading>,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    if (readings.size < 2) return

    val targetMin = profile.targetGlucoseMin
    val targetMax = profile.targetGlucoseMax
    val isMmol = profile.glucoseUnit == "mmol/L"

    val sortedReadings = remember(readings) {
        readings.sortedBy { it.dateTimeMillis }.takeLast(20)
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val minVal = remember(sortedReadings, targetMin) {
        ((sortedReadings.minOfOrNull { it.readingValue } ?: 50.0).coerceAtMost(targetMin) - 15.0).coerceAtLeast(0.0)
    }
    val maxVal = remember(sortedReadings, targetMax) {
        (sortedReadings.maxOfOrNull { it.readingValue } ?: 200.0).coerceAtLeast(targetMax) + 20.0
    }
    val valRange = (maxVal - minVal).coerceAtLeast(1.0)

    val primaryColor = MaterialTheme.colorScheme.primary
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Glucose Trend Curve",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val lastReading = sortedReadings.lastOrNull()
                    if (lastReading != null) {
                        Text(
                            text = "Latest: ${lastReading.readingValue} ${profile.glucoseUnit} (${lastReading.mealContext.ifEmpty { "Logged" }})",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                lastReading.readingValue < targetMin -> Color(0xFFEF4444)
                                lastReading.readingValue > targetMax -> Color(0xFFF59E0B)
                                else -> Color(0xFF10B981)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (selectedIndex != null && selectedIndex!! in sortedReadings.indices) {
                    val sel = sortedReadings[selectedIndex!!]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${sel.readingValue} ${profile.glucoseUnit} • ${dateFormat.format(Date(sel.dateTimeMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Curve Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(sortedReadings) {
                        detectTapGestures(
                            onTap = { offset ->
                                val stepX = size.width.toFloat() / (sortedReadings.size - 1).coerceAtLeast(1)
                                val idx = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, sortedReadings.size - 1)
                                selectedIndex = if (selectedIndex == idx) null else idx
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val padY = 16.dp.toPx()
                val usableH = h - padY * 2

                // Helper to map (index, readingValue) to Offset
                fun getPoint(idx: Int, value: Double): Offset {
                    val x = if (sortedReadings.size > 1) idx.toFloat() / (sortedReadings.size - 1) * w else w / 2f
                    val normY = ((value - minVal) / valRange).toFloat().coerceIn(0f, 1f)
                    val y = padY + (1f - normY) * usableH
                    return Offset(x, y)
                }

                // 1. Draw Target Safe-Zone Band (70 - 180)
                val targetTopY = getPoint(0, targetMax).y
                val targetBottomY = getPoint(0, targetMin).y
                drawRect(
                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                    topLeft = Offset(0f, targetTopY),
                    size = Size(w, (targetBottomY - targetTopY).coerceAtLeast(1f))
                )

                // Dashed target boundary lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.35f),
                    start = Offset(0f, targetTopY),
                    end = Offset(w, targetTopY),
                    pathEffect = dashEffect,
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.35f),
                    start = Offset(0f, targetBottomY),
                    end = Offset(w, targetBottomY),
                    pathEffect = dashEffect,
                    strokeWidth = 1.5f
                )

                // 2. Build Smooth Bezier Path
                val points = sortedReadings.mapIndexed { idx, item -> getPoint(idx, item.readingValue) }
                if (points.size > 1) {
                    val linePath = Path()
                    val fillPath = Path()

                    linePath.moveTo(points.first().x, points.first().y)
                    fillPath.moveTo(points.first().x, h)
                    fillPath.lineTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        linePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(points.last().x, h)
                    fillPath.close()

                    // Gradient underfill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            startY = padY,
                            endY = h
                        )
                    )

                    // Line Stroke
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 3. Draw Points & Selection Highlight
                points.forEachIndexed { idx, pt ->
                    val reading = sortedReadings[idx]
                    val pointColor = when {
                        reading.readingValue < targetMin -> Color(0xFFEF4444)
                        reading.readingValue > targetMax -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }

                    val isSelected = selectedIndex == idx
                    if (isSelected) {
                        // Halo around selected point
                        drawCircle(
                            color = pointColor.copy(alpha = 0.25f),
                            radius = 12.dp.toPx(),
                            center = pt
                        )
                        // Vertical guideline
                        drawLine(
                            color = pointColor.copy(alpha = 0.5f),
                            start = Offset(pt.x, 0f),
                            end = Offset(pt.x, h),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                    }

                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = pointColor,
                        radius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}
