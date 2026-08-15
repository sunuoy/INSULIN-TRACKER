package com.example.ui.screens.reports

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.charts.TimeInRangeCard
import com.example.ui.viewmodel.GlucoViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: GlucoViewModel) {
    val rawInsulin by viewModel.insulinRecords.collectAsStateWithLifecycle()
    val rawGlucose by viewModel.glucoseReadings.collectAsStateWithLifecycle()
    val rawBp by viewModel.bloodPressureRecords.collectAsStateWithLifecycle()
    val rawRefills by viewModel.refillLogs.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    val pdfIncludeGlucose by viewModel.pdfIncludeGlucose.collectAsStateWithLifecycle()
    val pdfIncludeInsulin by viewModel.pdfIncludeInsulin.collectAsStateWithLifecycle()
    val pdfIncludeBp by viewModel.pdfIncludeBp.collectAsStateWithLifecycle()
    val pdfIncludeRefills by viewModel.pdfIncludeRefills.collectAsStateWithLifecycle()
    val pdfDateRange by viewModel.pdfDateRange.collectAsStateWithLifecycle()
    val pdfCustomFromDate by viewModel.pdfCustomFromDate.collectAsStateWithLifecycle()
    val pdfCustomToDate by viewModel.pdfCustomToDate.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Period Filter State (7, 14, 30, 90 days)
    var selectedPeriodDays by remember { mutableIntStateOf(30) }

    val periodCutoffMillis = remember(selectedPeriodDays) {
        System.currentTimeMillis() - (selectedPeriodDays.toLong() * 24L * 60L * 60L * 1000L)
    }

    val periodGlucose = remember(rawGlucose, periodCutoffMillis) {
        rawGlucose.filter { it.dateTimeMillis >= periodCutoffMillis }
    }
    val periodInsulin = remember(rawInsulin, periodCutoffMillis) {
        rawInsulin.filter { it.dateTimeMillis >= periodCutoffMillis }
    }

    // Dynamic metrics for the selected period
    val isMmolVal = profile.glucoseUnit == "mmol/L"
    val periodGlucoseAvg = remember(periodGlucose) {
        if (periodGlucose.isNotEmpty()) periodGlucose.map { it.readingValue }.average() else 0.0
    }
    val periodInsulinTotal = remember(periodInsulin) {
        periodInsulin.sumOf { it.doseUnits }
    }

    // Estimated HbA1c Calculation (ADA standard formula: eA1C = (average mg/dL + 46.7) / 28.7)
    val periodAvgMgDl = if (isMmolVal) periodGlucoseAvg * 18.0182 else periodGlucoseAvg
    val estimatedA1c = if (periodAvgMgDl > 30.0) (periodAvgMgDl + 46.7) / 28.7 else 0.0

    // Time-In-Range percentages
    val targetMin = profile.targetGlucoseMin
    val targetMax = profile.targetGlucoseMax
    val totalPeriodReadings = periodGlucose.size
    val lowCount = remember(periodGlucose, targetMin) { periodGlucose.count { it.readingValue < targetMin } }
    val inRangeCount = remember(periodGlucose, targetMin, targetMax) { periodGlucose.count { it.readingValue in targetMin..targetMax } }
    val highCount = remember(periodGlucose, targetMax) { periodGlucose.count { it.readingValue > targetMax } }

    val pctLow = if (totalPeriodReadings > 0) (lowCount.toDouble() / totalPeriodReadings) * 100.0 else 0.0
    val pctIn = if (totalPeriodReadings > 0) (inRangeCount.toDouble() / totalPeriodReadings) * 100.0 else 0.0

    // Circadian / 24-Hour Time-of-Day Glucose Breakdown
    val timeOfDayStats = remember(periodGlucose) {
        val overnight = periodGlucose.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTimeMillis }
            cal.get(Calendar.HOUR_OF_DAY) in 0..5
        }
        val morning = periodGlucose.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTimeMillis }
            cal.get(Calendar.HOUR_OF_DAY) in 6..11
        }
        val afternoon = periodGlucose.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTimeMillis }
            cal.get(Calendar.HOUR_OF_DAY) in 12..17
        }
        val evening = periodGlucose.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTimeMillis }
            cal.get(Calendar.HOUR_OF_DAY) in 18..23
        }

        listOf(
            "Overnight" to overnight,
            "Morning" to morning,
            "Afternoon" to afternoon,
            "Evening" to evening
        )
    }

    fun shareExportedFile(file: File, mimeType: String, chooserTitle: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportCsvReport(): File {
        val file = File(context.cacheDir, "gluco_report_${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { writer ->
            writer.write("Category,Timestamp,Date Time,Value,Unit,Context/Type,Notes\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            rawGlucose.forEach {
                writer.write("Glucose,${it.dateTimeMillis},${sdf.format(Date(it.dateTimeMillis))},${it.readingValue},${profile.glucoseUnit},${it.mealContext},\"${it.notes}\"\n")
            }
            rawInsulin.forEach {
                writer.write("Insulin,${it.dateTimeMillis},${sdf.format(Date(it.dateTimeMillis))},${it.doseUnits},Units,${it.insulinType},\"${it.notes}\"\n")
            }
            rawBp.forEach {
                writer.write("Blood Pressure,${it.dateTimeMillis},${sdf.format(Date(it.dateTimeMillis))},${it.systolic}/${it.diastolic} (Pulse: ${it.pulse}),mmHg,Vitals,\"${it.notes}\"\n")
            }
        }
        return file
    }

    fun showDatePicker(initialDateStr: String, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val parts = initialDateStr.split("-")
        if (parts.size == 3) {
            try {
                calendar.set(Calendar.YEAR, parts[0].toInt())
                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            } catch (e: Exception) {}
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Column {
                Text(
                    text = "Clinical Analytics & Reports",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Comprehensive glycemic patterns, eA1c, and doctor reports",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Modern Material 3 Multi-Period Selector Bar (7D, 14D, 30D, 90D)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val periods = listOf(7 to "7 Days", 14 to "14 Days", 30 to "30 Days", 90 to "90 Days")
                    periods.forEach { (days, label) ->
                        val isSelected = selectedPeriodDays == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedPeriodDays = days }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Estimated HbA1c (eA1c) & Glycemic Target Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                    .size(36.dp)
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Biotech,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Estimated HbA1c (eA1c)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Based on ${selectedPeriodDays}d glucose records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        val a1cStatusColor = when {
                            estimatedA1c <= 0.0 -> Color.Gray
                            estimatedA1c < 6.5 -> Color(0xFF10B981) // Optimal
                            estimatedA1c <= 7.0 -> Color(0xFF059669) // ADA Goal (<7%)
                            estimatedA1c <= 8.0 -> Color(0xFFF59E0B) // Warning
                            else -> Color(0xFFEF4444) // Elevated
                        }

                        val a1cStatusText = when {
                            estimatedA1c <= 0.0 -> "N/A"
                            estimatedA1c < 6.5 -> "Optimal"
                            estimatedA1c <= 7.0 -> "ADA Target"
                            estimatedA1c <= 8.0 -> "Moderate"
                            else -> "Elevated"
                        }

                        Box(
                            modifier = Modifier
                                .background(a1cStatusColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, a1cStatusColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = a1cStatusText,
                                color = a1cStatusColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = if (estimatedA1c > 0) String.format(Locale.US, "%.1f%%", estimatedA1c) else "N/A",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Avg: ${if (periodGlucoseAvg > 0) String.format(Locale.US, "%.1f %s", periodGlucoseAvg, profile.glucoseUnit) else "N/A"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", periodInsulinTotal)} u",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total Insulin (${selectedPeriodDays}d)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Visual eA1c Reference Gauge
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        ) {
                            val w = size.width
                            val h = size.height
                            drawRect(Color(0xFF10B981), Offset(0f, 0f), Size(w * 0.35f, h))
                            drawRect(Color(0xFF06B6D4), Offset(w * 0.35f, 0f), Size(w * 0.2f, h))
                            drawRect(Color(0xFFF59E0B), Offset(w * 0.55f, 0f), Size(w * 0.25f, h))
                            drawRect(Color(0xFFEF4444), Offset(w * 0.8f, 0f), Size(w * 0.2f, h))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("< 6.5%", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            Text("7.0% Goal", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                            Text("8.0%", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            Text("> 9.0%", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }

        // 3. Clinical Time-In-Range (TIR) Segmented Progress Card
        item {
            TimeInRangeCard(
                glucoseList = periodGlucose,
                profile = profile
            )
        }

        // 4. 24-Hour Time-of-Day Circadian Glucose Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                                    .size(36.dp)
                                    .background(Color(0xFF0EA5E9).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF0EA5E9),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "24-Hour Time-of-Day Patterns",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Glycemic control by daily time window",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // 4 Quadrants Grid (Overnight, Morning, Afternoon, Evening)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        timeOfDayStats.take(2).forEach { (title, list) ->
                            val avg = if (list.isNotEmpty()) list.map { it.readingValue }.average() else 0.0
                            val icon = if (title == "Overnight") Icons.Default.Bedtime else Icons.Default.WbSunny
                            TimeOfDayBox(
                                title = title,
                                subtitle = if (title == "Overnight") "00:00–06:00" else "06:00–12:00",
                                avg = avg,
                                count = list.size,
                                unit = profile.glucoseUnit,
                                icon = icon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        timeOfDayStats.drop(2).forEach { (title, list) ->
                            val avg = if (list.isNotEmpty()) list.map { it.readingValue }.average() else 0.0
                            val icon = if (title == "Afternoon") Icons.Default.WbTwilight else Icons.Default.NightsStay
                            TimeOfDayBox(
                                title = title,
                                subtitle = if (title == "Afternoon") "12:00–18:00" else "18:00–24:00",
                                avg = avg,
                                count = list.size,
                                unit = profile.glucoseUnit,
                                icon = icon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 5. Automated Smart Clinical Insights
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Smart Clinical Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (pctIn >= 70.0) {
                            InsightBullet(
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF10B981),
                                text = "ADA Gold Standard Achieved: Time In Range is ${String.format(Locale.US, "%.0f%%", pctIn)} (Target ≥ 70%). Excellent glycemic stability."
                            )
                        } else if (totalPeriodReadings > 0) {
                            InsightBullet(
                                icon = Icons.Default.Info,
                                color = Color(0xFFF59E0B),
                                text = "Time In Range is currently ${String.format(Locale.US, "%.0f%%", pctIn)}. Aim for ≥ 70% in-target readings."
                            )
                        }

                        if (pctLow > 4.0) {
                            InsightBullet(
                                icon = Icons.Default.Warning,
                                color = Color(0xFFEF4444),
                                text = "Hypoglycemia Alert: Low readings accounted for ${String.format(Locale.US, "%.1f%%", pctLow)} (ADA safety threshold ≤ 4%). Verify basal rates."
                            )
                        } else {
                            InsightBullet(
                                icon = Icons.Default.Shield,
                                color = Color(0xFF06B6D4),
                                text = "Hypoglycemia Protection: Low readings remain safely below the 4% clinical safety limit."
                            )
                        }

                        if (periodInsulin.isNotEmpty() && periodGlucose.isNotEmpty()) {
                            InsightBullet(
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFF8B5CF6),
                                text = "Daily Insulin Ratio: Averaging ${String.format(Locale.US, "%.1f u", periodInsulinTotal / selectedPeriodDays.toDouble())} total daily insulin."
                            )
                        }
                    }
                }
            }
        }

        // 6. Doctor PDF & CSV Export Builder Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("pdf_report_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "Doctor Clinical Report Export",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Generate formal PDF/CSV reports for physician visits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    HorizontalDivider()

                    // Filter Date Range section
                    Text(
                        text = "Report Date Range:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val ranges = listOf("Last 7 Days", "Last 14 Days", "Last 30 Days", "All", "Custom Range")
                        ranges.forEach { range ->
                            FilterChip(
                                selected = pdfDateRange == range,
                                onClick = { viewModel.setPdfDateRange(range) },
                                label = { Text(range, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (pdfDateRange == range) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = pdfDateRange == "Custom Range",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = pdfCustomFromDate,
                                onValueChange = { viewModel.setPdfCustomFromDate(it) },
                                label = { Text("From (YYYY-MM-DD)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showDatePicker(pdfCustomFromDate) { viewModel.setPdfCustomFromDate(it) }
                                    }) {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = pdfCustomToDate,
                                onValueChange = { viewModel.setPdfCustomToDate(it) },
                                label = { Text("To (YYYY-MM-DD)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showDatePicker(pdfCustomToDate) { viewModel.setPdfCustomToDate(it) }
                                    }) {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }

                    // Choose Log categories to include
                    Text(
                        text = "Include Records:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = pdfIncludeGlucose,
                            onClick = { viewModel.setPdfIncludeGlucose(!pdfIncludeGlucose) },
                            label = { Text("Glucose", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = pdfIncludeInsulin,
                            onClick = { viewModel.setPdfIncludeInsulin(!pdfIncludeInsulin) },
                            label = { Text("Insulin", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = pdfIncludeBp,
                            onClick = { viewModel.setPdfIncludeBp(!pdfIncludeBp) },
                            label = { Text("Blood Pressure", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = pdfIncludeRefills,
                            onClick = { viewModel.setPdfIncludeRefills(!pdfIncludeRefills) },
                            label = { Text("Refills", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Export Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val file = viewModel.generatePdfReport(
                                        records = rawInsulin,
                                        readings = rawGlucose,
                                        bpRecords = rawBp,
                                        refills = rawRefills,
                                        profile = profile
                                    )
                                    shareExportedFile(file, "application/pdf", "Share Doctor PDF Report")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error generating report: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF Report")
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val file = exportCsvReport()
                                    shareExportedFile(file, "text/csv", "Share CSV Log Data")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error exporting CSV: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeOfDayBox(
    title: String,
    subtitle: String,
    avg: Double,
    count: Int,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (avg > 0) String.format(Locale.US, "%.1f %s", avg, unit) else "No Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (avg > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
            Text(
                text = "$count readings",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightBullet(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 16.sp
        )
    }
}
