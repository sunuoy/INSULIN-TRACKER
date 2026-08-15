package com.example.ui.screens.history

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.cards.*
import com.example.ui.viewmodel.GlucoViewModel
import java.text.SimpleDateFormat
import java.util.*

sealed class TimelineItem(val timestamp: Long) {
    data class Glucose(val reading: GlucoseReading) : TimelineItem(reading.dateTimeMillis)
    data class Insulin(val record: InsulinRecord) : TimelineItem(record.dateTimeMillis)
    data class BloodPressure(val record: BloodPressureRecord) : TimelineItem(record.dateTimeMillis)
    data class Refill(val log: CartridgeRefillLog) : TimelineItem(log.dateTimeMillis)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: GlucoViewModel,
    onEditInsulin: (InsulinRecord) -> Unit,
    onEditGlucose: (GlucoseReading) -> Unit,
    onEditRefill: (CartridgeRefillLog) -> Unit,
    onEditBloodPressure: (BloodPressureRecord) -> Unit,
    onAddInsulinClick: () -> Unit,
    onAddGlucoseClick: () -> Unit,
    onAddRefillClick: () -> Unit,
    onAddBloodPressureClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = All Timeline, 1 = Glucose, 2 = Insulin, 3 = BP, 4 = Refills

    val insulinList by viewModel.filteredInsulinRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val glucoseList by viewModel.filteredGlucoseReadings.collectAsStateWithLifecycle(initialValue = emptyList())
    val bpList by viewModel.filteredBloodPressureRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val refillLogs by viewModel.refillLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    val insulinFilter by viewModel.insulinTypeFilter.collectAsStateWithLifecycle()
    val glucoseFilter by viewModel.mealContextFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var bpFilter by remember { mutableStateOf("All") }
    var allQuickFilter by remember { mutableStateOf("All") } // "All", "Out of Range", "Glucose", "Insulin", "BP"

    val filteredBpList = remember(bpList, bpFilter) {
        when (bpFilter) {
            "Normal" -> bpList.filter { it.systolic < 120 && it.diastolic < 80 }
            "Elevated" -> bpList.filter { (it.systolic in 120..129) && it.diastolic < 80 }
            "High BP" -> bpList.filter { it.systolic >= 130 || it.diastolic >= 80 }
            "Low BP" -> bpList.filter { it.systolic < 90 || it.diastolic < 60 }
            else -> bpList
        }
    }

    val filteredRefillLogs = remember(refillLogs, searchQuery) {
        if (searchQuery.isBlank()) {
            refillLogs
        } else {
            refillLogs.filter { log ->
                log.actionType.contains(searchQuery, ignoreCase = true) ||
                log.capacity.toString().contains(searchQuery)
            }
        }
    }

    // Unified Chronological Timeline Items
    val unifiedTimeline = remember(glucoseList, insulinList, bpList, refillLogs, allQuickFilter, searchQuery, profile) {
        val items = mutableListOf<TimelineItem>()
        if (allQuickFilter == "All" || allQuickFilter == "Glucose" || allQuickFilter == "Out of Range") {
            val list = if (allQuickFilter == "Out of Range") {
                glucoseList.filter { it.readingValue < profile.targetGlucoseMin || it.readingValue > profile.targetGlucoseMax }
            } else {
                glucoseList
            }
            items.addAll(list.map { TimelineItem.Glucose(it) })
        }
        if (allQuickFilter == "All" || allQuickFilter == "Insulin") {
            items.addAll(insulinList.map { TimelineItem.Insulin(it) })
        }
        if (allQuickFilter == "All" || allQuickFilter == "BP" || allQuickFilter == "Out of Range") {
            val list = if (allQuickFilter == "Out of Range") {
                bpList.filter { it.systolic >= 130 || it.diastolic >= 80 || it.systolic < 90 }
            } else {
                bpList
            }
            items.addAll(list.map { TimelineItem.BloodPressure(it) })
        }
        if (allQuickFilter == "All") {
            items.addAll(refillLogs.map { TimelineItem.Refill(it) })
        }

        // Apply text search if not blank
        val searchFiltered = if (searchQuery.isNotBlank()) {
            items.filter { item ->
                when (item) {
                    is TimelineItem.Glucose -> item.reading.notes.contains(searchQuery, ignoreCase = true) || item.reading.mealContext.contains(searchQuery, ignoreCase = true)
                    is TimelineItem.Insulin -> item.record.notes.contains(searchQuery, ignoreCase = true) || item.record.insulinType.contains(searchQuery, ignoreCase = true)
                    is TimelineItem.BloodPressure -> item.record.notes.contains(searchQuery, ignoreCase = true)
                    is TimelineItem.Refill -> item.log.actionType.contains(searchQuery, ignoreCase = true)
                }
            }
        } else {
            items
        }

        searchFiltered.sortedByDescending { it.timestamp }
    }

    // Grouping by Date (Day key)
    val groupedTimeline = remember(unifiedTimeline) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        unifiedTimeline.groupBy { dateFormat.format(Date(it.timestamp)) }
    }

    fun formatDayTitle(dayKey: String, sampleTimestamp: Long): String {
        val now = Calendar.getInstance()
        val itemCal = Calendar.getInstance().apply { timeInMillis = sampleTimestamp }
        return when {
            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR) -> "Today"
            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
            else -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(sampleTimestamp))
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> onAddGlucoseClick()
                        1 -> onAddGlucoseClick()
                        2 -> onAddInsulinClick()
                        3 -> onAddBloodPressureClick()
                        else -> onAddRefillClick()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("history_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = when (selectedTab) {
                        1 -> "Add Glucose Reading"
                        2 -> "Add Insulin Log"
                        3 -> "Add Blood Pressure"
                        4 -> "Add Cartridge Refill"
                        else -> "Quick Add Log"
                    }
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("history_screen")
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Clinical Track Logs",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Chronological history & biometric tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Search Bar Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("history_search_input"),
                placeholder = { Text("Search logs by notes, type, or tag...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear text")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp)
            )

            // Modern Scrollable Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = listOf("All Feed", "Glucose", "Insulin", "Blood Pressure", "Refills")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Context Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                when (selectedTab) {
                    0 -> {
                        // All feed filters
                        listOf("All", "⚠️ Out of Range", "Glucose", "Insulin", "BP").forEach { filter ->
                            val active = allQuickFilter == filter
                            FilterChip(
                                selected = active,
                                onClick = { allQuickFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (active) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                    1 -> {
                        // Glucose Filters
                        listOf("All", "Fasting", "Before Meal", "After Meal", "Bedtime").forEach { filter ->
                            val active = glucoseFilter == filter
                            FilterChip(
                                selected = active,
                                onClick = { viewModel.setMealContextFilter(filter) },
                                label = { Text(filter, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (active) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                    2 -> {
                        // Insulin Filters
                        listOf("All", "Rapid-acting", "Long-acting", "Intermediate", "Short-acting").forEach { filter ->
                            val active = insulinFilter == filter
                            FilterChip(
                                selected = active,
                                onClick = { viewModel.setInsulinFilter(filter) },
                                label = { Text(filter, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (active) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                    3 -> {
                        // BP Filters
                        listOf("All", "Normal", "Elevated", "High BP", "Low BP").forEach { filter ->
                            val active = bpFilter == filter
                            FilterChip(
                                selected = active,
                                onClick = { bpFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (active) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                    4 -> {
                        // Refill clear
                        if (filteredRefillLogs.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearAllRefillLogs() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear History", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Tab Content Views
            when (selectedTab) {
                0 -> {
                    // Unified Timeline Feed with Grouped Date Headers
                    if (unifiedTimeline.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.Timeline,
                            title = "No Log Entries Found",
                            subtitle = "Start logging your daily glucose readings, insulin injections, and vitals to see your chronological health timeline here.",
                            buttonText = "Quick Log Glucose",
                            onButtonClick = onAddGlucoseClick
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            groupedTimeline.forEach { (dayKey, itemsForDay) ->
                                val sampleTime = itemsForDay.first().timestamp
                                val dayTitle = formatDayTitle(dayKey, sampleTime)

                                // Calculate daily aggregates
                                val dayGlucose = itemsForDay.filterIsInstance<TimelineItem.Glucose>()
                                val dayInsulin = itemsForDay.filterIsInstance<TimelineItem.Insulin>()
                                val avgSugar = if (dayGlucose.isNotEmpty()) dayGlucose.map { it.reading.readingValue }.average() else 0.0
                                val totalInsulin = dayInsulin.sumOf { it.record.doseUnits }

                                stickyHeader {
                                    Surface(
                                        color = MaterialTheme.colorScheme.background,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dayTitle,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (avgSugar > 0) {
                                                    Text(
                                                        text = "Avg: ${String.format(Locale.US, "%.0f", avgSugar)} ${profile.glucoseUnit}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                if (totalInsulin > 0) {
                                                    Text(
                                                        text = "• ${String.format(Locale.US, "%.1fu", totalInsulin)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                items(itemsForDay, key = { item ->
                                    when (item) {
                                        is TimelineItem.Glucose -> "g_${item.reading.id}"
                                        is TimelineItem.Insulin -> "i_${item.record.id}"
                                        is TimelineItem.BloodPressure -> "bp_${item.record.id}"
                                        is TimelineItem.Refill -> "r_${item.log.id}"
                                    }
                                }) { item ->
                                    when (item) {
                                        is TimelineItem.Glucose -> GlucoseReadingCard(
                                            reading = item.reading,
                                            profile = profile,
                                            onEdit = { onEditGlucose(item.reading) },
                                            onDelete = { viewModel.deleteGlucoseReading(item.reading) }
                                        )
                                        is TimelineItem.Insulin -> InsulinRecordCard(
                                            rec = item.record,
                                            onEdit = { onEditInsulin(item.record) },
                                            onDelete = { viewModel.deleteInsulinRecord(item.record) }
                                        )
                                        is TimelineItem.BloodPressure -> BloodPressureRecordCard(
                                            record = item.record,
                                            onEdit = { onEditBloodPressure(item.record) },
                                            onDelete = { viewModel.deleteBloodPressureRecord(item.record) }
                                        )
                                        is TimelineItem.Refill -> RefillLogCard(
                                            log = item.log,
                                            onEdit = { onEditRefill(item.log) },
                                            onDelete = { viewModel.deleteRefillLog(item.log) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Glucose Only List
                    if (glucoseList.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.WaterDrop,
                            title = "No Blood Sugar Readings",
                            subtitle = "Track your fasting, pre-meal, and post-meal glucose logs to manage diabetes targets.",
                            buttonText = "Log Glucose",
                            onButtonClick = onAddGlucoseClick
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(glucoseList, key = { it.id }) { reading ->
                                GlucoseReadingCard(
                                    reading = reading,
                                    profile = profile,
                                    onEdit = { onEditGlucose(reading) },
                                    onDelete = { viewModel.deleteGlucoseReading(reading) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Insulin Only List
                    if (insulinList.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.Vaccines,
                            title = "No Insulin Dose Logs",
                            subtitle = "Keep track of rapid, short, or long-acting insulin doses to manage glucose levels effectively.",
                            buttonText = "Log Insulin Dose",
                            onButtonClick = onAddInsulinClick
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(insulinList, key = { it.id }) { record ->
                                InsulinRecordCard(
                                    rec = record,
                                    onEdit = { onEditInsulin(record) },
                                    onDelete = { viewModel.deleteInsulinRecord(record) }
                                )
                            }
                        }
                    }
                }
                3 -> {
                    // Blood Pressure Only List
                    if (filteredBpList.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.Favorite,
                            title = "No Blood Pressure Records",
                            subtitle = "Logging systolic/diastolic blood pressure readings helps keep track of cardiovascular health.",
                            buttonText = "Log Blood Pressure",
                            onButtonClick = onAddBloodPressureClick
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredBpList, key = { it.id }) { record ->
                                BloodPressureRecordCard(
                                    record = record,
                                    onEdit = { onEditBloodPressure(record) },
                                    onDelete = { viewModel.deleteBloodPressureRecord(record) }
                                )
                            }
                        }
                    }
                }
                4 -> {
                    // Refills List
                    if (filteredRefillLogs.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.Refresh,
                            title = "No Refill Logs",
                            subtitle = "Record cartridge replacements or pen refills to ensure accurate remaining-insulin capacity.",
                            buttonText = "Log Refill",
                            onButtonClick = onAddRefillClick
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredRefillLogs, key = { it.id }) { log ->
                                RefillLogCard(
                                    log = log,
                                    onEdit = { onEditRefill(log) },
                                    onDelete = { viewModel.deleteRefillLog(log) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = onButtonClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
