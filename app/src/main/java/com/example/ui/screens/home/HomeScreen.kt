package com.example.ui.screens.home

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.*
import com.example.ui.components.cards.*
import com.example.ui.components.common.*
import com.example.ui.components.dialogs.*
import com.example.ui.screens.auth.*
import com.example.ui.components.charts.*
import com.example.ui.screens.history.*
import com.example.ui.screens.home.*
import com.example.ui.screens.profile.*
import com.example.ui.screens.reminders.*
import com.example.ui.screens.reports.*
import com.example.ui.screens.settings.*
import com.example.ui.screens.steps.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.GlucoViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: GlucoViewModel,
    onLogInsulinClick: () -> Unit,
    onLogGlucoseClick: () -> Unit,
    onRefillClick: () -> Unit,
    onLogBloodPressureClick: () -> Unit,
    onLogStepsClick: () -> Unit = {}
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val rawInsulin by viewModel.insulinRecords.collectAsStateWithLifecycle()
    val rawGlucose by viewModel.glucoseReadings.collectAsStateWithLifecycle()
    val rawBp by viewModel.bloodPressureRecords.collectAsStateWithLifecycle()
    val remindersList by viewModel.reminders.collectAsStateWithLifecycle()

    val latestBp = remember(rawBp) { rawBp.maxByOrNull { it.dateTimeMillis } }
    val latestGlucose = remember(rawGlucose) { rawGlucose.maxByOrNull { it.dateTimeMillis } }

    // --- Performance Optimizations & Dashboard Feeds (Smooth scrolling, cached results) ---
    val reportsData = remember(rawInsulin, rawGlucose, profile) {
        viewModel.getReportsData(rawInsulin, rawGlucose, profile)
    }

    val startOfToday = remember(rawGlucose, rawInsulin) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayGlucoseReadings = remember(rawGlucose, startOfToday) {
        rawGlucose.filter { it.dateTimeMillis >= startOfToday }.sortedByDescending { it.dateTimeMillis }
    }

    val fallbackGlucoseReadings = remember(rawGlucose) {
        rawGlucose.sortedByDescending { it.dateTimeMillis }.take(3)
    }

    val todayInsulinRecords = remember(rawInsulin, startOfToday) {
        rawInsulin.filter { it.dateTimeMillis >= startOfToday }.sortedByDescending { it.dateTimeMillis }
    }

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    var showProfileEditDialog by remember { mutableStateOf(false) }
    val enabledReminders = remember(remindersList) { remindersList.filter { it.isEnabled }.take(3) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Welcome Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4F46E5), // Indigo
                                Color(0xFF06B6D4)  // Cyan
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val context = LocalContext.current
                            val prefs = remember { context.getSharedPreferences("gluco_auth_prefs", android.content.Context.MODE_PRIVATE) }
                            val avatarIndex = prefs.getInt("profile_avatar_${profile.id}", 0)
                            val avatarOptions = listOf(
                                Icons.Default.MedicalServices to Color(0xFF1E88E5), // Blue
                                Icons.Default.Person to Color(0xFF8E24AA),          // Purple
                                Icons.Default.Favorite to Color(0xFFE53935),        // Red
                                Icons.Default.CloudQueue to Color(0xFF00ACC1),      // Teal cyan
                                Icons.Default.HealthAndSafety to Color(0xFF43A047), // Green
                                Icons.Default.LocalHospital to Color(0xFFFB8C00)    // Orange
                            )
                            val (avatarIcon, avatarColor) = avatarOptions.getOrElse(avatarIndex) { avatarOptions[0] }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(avatarColor, androidx.compose.foundation.shape.CircleShape)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = avatarIcon,
                                    contentDescription = "Active Profile Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Clean Stable Health",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "Hello, ${profile.userName.ifEmpty { "Health Champion" }}!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = { showProfileEditDialog = true },
                            modifier = Modifier.size(36.dp).testTag("home_edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile Quick-Action",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Target Range: ${profile.targetGlucoseMin.toInt()}-${profile.targetGlucoseMax.toInt()} ${profile.glucoseUnit}. All entries persist locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 1. Clinical Alert Banners Section (Hypo, Hyper, Stage 2 BP warnings)
        item {
            VitalAlertBannersSection(
                latestGlucose = latestGlucose,
                latestBp = latestBp,
                profile = profile
            )
        }

        // 2. Clinical Time in Range (TIR) Card
        item {
            TimeInRangeCard(
                glucoseList = rawGlucose,
                profile = profile
            )
        }

        // 3. Interactive Glucose Trend Sparkline & Curve Chart
        if (rawGlucose.size >= 2) {
            item {
                InteractiveGlucoseTrendChart(
                    readings = rawGlucose,
                    profile = profile
                )
            }
        }

        // Action Buttons Grid-break
        item {
            val vaccinesTransition = rememberInfiniteTransition(label = "vaccines")
            val vaccinesRotation by vaccinesTransition.animateFloat(
                initialValue = -8f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "vaccinesRotation"
            )

            val waterTransition = rememberInfiniteTransition(label = "water")
            val waterTranslationY by waterTransition.animateFloat(
                initialValue = -3f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "waterTranslationY"
            )

            val heartTransition = rememberInfiniteTransition(label = "heart")
            val heartScale by heartTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "heartScale"
            )

            val refreshTransition = rememberInfiniteTransition(label = "refresh")
            val refreshRotation by refreshTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "refreshRotation"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    // Log Glucose Button Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clickable { onLogGlucoseClick() }
                            .testTag("home_add_glucose_button"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val curveColor = Color(0xFFFB8C00)
                            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, size.height * 0.65f)
                                    cubicTo(
                                        size.width * 0.35f, size.height * 0.55f,
                                        size.width * 0.65f, size.height * 0.85f,
                                        size.width, size.height * 0.5f
                                    )
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            curveColor.copy(alpha = 0.12f)
                                        )
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(31.dp)
                                        .background(Color(0xFFFB8C00), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = "Glucose Icon",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .graphicsLayer {
                                                translationY = waterTranslationY
                                            }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Glucose Level", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Log Sugar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // Log Insulin Button Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clickable { onLogInsulinClick() }
                            .testTag("home_add_insulin_button"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val curveColor = Color(0xFF3F51B5)
                            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, size.height * 0.65f)
                                    cubicTo(
                                        size.width * 0.35f, size.height * 0.55f,
                                        size.width * 0.65f, size.height * 0.85f,
                                        size.width, size.height * 0.5f
                                    )
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            curveColor.copy(alpha = 0.12f)
                                        )
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(31.dp)
                                        .background(Color(0xFF3F51B5), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Vaccines,
                                        contentDescription = "Insulin Icon",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .graphicsLayer {
                                                rotationZ = vaccinesRotation
                                            }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Insulin Dose", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Record Units", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    // Log Cartridge Refill Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clickable { onRefillClick() }
                            .testTag("home_add_refill_button"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val curveColor = Color(0xFF00ACC1)
                            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, size.height * 0.65f)
                                    cubicTo(
                                        size.width * 0.35f, size.height * 0.55f,
                                        size.width * 0.65f, size.height * 0.85f,
                                        size.width, size.height * 0.5f
                                    )
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            curveColor.copy(alpha = 0.12f)
                                        )
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(31.dp)
                                        .background(Color(0xFF00ACC1), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refill Icon",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .graphicsLayer {
                                                rotationZ = refreshRotation
                                            }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cartridge Refill", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Change Capacity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // Log Blood Pressure Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clickable { onLogBloodPressureClick() }
                            .testTag("home_add_bp_button"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val curveColor = Color(0xFFE53935)
                            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, size.height * 0.65f)
                                    cubicTo(
                                        size.width * 0.35f, size.height * 0.55f,
                                        size.width * 0.65f, size.height * 0.85f,
                                        size.width, size.height * 0.5f
                                    )
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            curveColor.copy(alpha = 0.12f)
                                        )
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(31.dp)
                                        .background(Color(0xFFE53935), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Blood Pressure Icon",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .graphicsLayer {
                                                scaleX = heartScale
                                                scaleY = heartScale
                                            }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Blood Pressure", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Systolic/Diastolic", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Low Cartridge / Change Refill Below 25 Units Reminder
        if (profile.cartridgeRemaining < 25.0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("low_cartridge_alert_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF991B1B), // Crimson Red
                                        Color(0xFF450A0A)  // Deep Burgundy
                                    )
                                )
                            )
                    ) {
                        // Custom background warning graphic (abstract warning wave curves)
                        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, size.height * 0.75f)
                                cubicTo(
                                    size.width * 0.35f, size.height * 0.6f,
                                    size.width * 0.65f, size.height * 0.9f,
                                    size.width, size.height * 0.5f
                                )
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(
                                path = path,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFEF4444).copy(alpha = 0.12f)
                                    )
                                )
                            )
                            // Draw decorative glowing circles on the right side
                            drawCircle(
                                color = Color.White.copy(alpha = 0.04f),
                                radius = size.height * 0.6f,
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.3f)
                            )
                        }

                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnimatedLowInsulinGraphic()
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "CRITICAL REMINDER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                                Text(
                                    "Insulin Cartridge Low!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Remaining level is ${profile.cartridgeRemaining.toInt()} Units (less than 25U threshold). Refill or change now to continue logged dosage safely.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onRefillClick() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF991B1B)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refill icon", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change & Refill Cartridge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Insulin Cartridge Balance Tracker Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                // 3D Shadow Layer (underneath)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 4.dp, y = 5.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                )

                // Main Floating Card Layer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.3f)
                            )
                        )
                    )
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Vaccines,
                                contentDescription = "Syringe Cartridge Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Insulin Cartridge Balance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Refill button
                        Button(
                            onClick = { onRefillClick() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refill",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refill / Change", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val capacity = if (profile.cartridgeCapacity > 0) profile.cartridgeCapacity else 300.0
                    val remaining = profile.cartridgeRemaining.coerceIn(0.0, capacity)
                    val percent = (remaining / capacity).coerceIn(0.0, 1.0)

                    val barColor = when {
                        percent < 0.20 -> Color(0xFFE53935) // Red warning
                        percent < 0.40 -> Color(0xFFFB8C00) // Orange warning
                        else -> MaterialTheme.colorScheme.primary
                    }

                    // Custom Glass Liquid Cartridge Graphic
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        
                        // 1. Draw glass background tube
                        drawRoundRect(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            topLeft = Offset(0f, 0f),
                            size = Size(w, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
                        )
                        
                        // Hazard warning stripes when cartridge is critically low
                        if (percent < 0.20) {
                            val stripeWidth = 12f
                            var startX = 0f
                            while (startX < w) {
                                drawLine(
                                    color = Color(0xFFEF4444).copy(alpha = 0.35f),
                                    start = Offset(startX, h),
                                    end = Offset(startX + stripeWidth, 0f),
                                    strokeWidth = 5f
                                )
                                drawLine(
                                    color = Color(0xFFFBBF24).copy(alpha = 0.35f),
                                    start = Offset(startX + stripeWidth, h),
                                    end = Offset(startX + stripeWidth * 2f, 0f),
                                    strokeWidth = 5f
                                )
                                startX += stripeWidth * 2f
                            }
                        }
                        
                        // 2. Draw liquid inside (if percent > 0)
                        if (percent > 0.0) {
                            val liquidWidth = w * percent.toFloat()
                            val liquidBrush = Brush.horizontalGradient(
                                colors = listOf(
                                    barColor.copy(alpha = 0.8f),
                                    barColor
                                )
                            )
                            drawRoundRect(
                                brush = liquidBrush,
                                topLeft = Offset(0f, 0f),
                                size = Size(liquidWidth, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
                            )
                        }
                        
                        // 3. Draw glass capsule sheen highlight (horizontal glossy reflection line on top half)
                        val sheenBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                        drawRoundRect(
                            brush = sheenBrush,
                            topLeft = Offset(2f, 2f),
                            size = Size(w - 4f, h * 0.35f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
                        )
                        
                        // 4. Draw outer glass tube border (pulsing warning outline when low)
                        val borderCol = if (percent < 0.20) Color(0xFFEF4444) else Color.LightGray.copy(alpha = 0.5f)
                        drawRoundRect(
                            color = borderCol,
                            topLeft = Offset(0f, 0f),
                            size = Size(w, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                        )
                        
                        // 5. Draw cartridge hash marks (representing units levels)
                        val totalTicks = 15
                        for (i in 1 until totalTicks) {
                            val tickX = w * (i.toFloat() / totalTicks)
                            val tickHeight = if (i % 5 == 0) h * 0.35f else h * 0.2f
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.3f),
                                start = Offset(tickX, 0f),
                                end = Offset(tickX, tickHeight),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.3f),
                                start = Offset(tickX, h),
                                end = Offset(tickX, h - tickHeight),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${remaining.toInt()} / ${capacity.toInt()} Units Left",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (percent < 0.20) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                        )

                        val statusText = when {
                            percent >= 0.95 -> "Full Cartridge"
                            percent < 0.15 -> "Critical! Change Cartridge"
                            percent < 0.35 -> "Low Balance"
                            else -> "Healthy Level"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (percent < 0.20) Color(0xFFE53935) else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

        // Today's Circular Dashboard Stats & Fresh Logs Dashboard
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                // 3D Shadow Layer (underneath)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 4.dp, y = 5.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                )

                // Main Floating Card Layer
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("clinical_dashboard_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.3f)
                            )
                        )
                    )
                ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val activeFeed = if (todayGlucoseReadings.isNotEmpty()) todayGlucoseReadings else fallbackGlucoseReadings

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Dashboard Analytics Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "Live Patient Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "TODAY'S INTENSITY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Circular Charts for fast clinical visual feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Average Glucose Ring
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(70.dp)) {
                                    // 1. Recessed background track
                                    drawCircle(
                                        color = Color.Black.copy(alpha = 0.18f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.05f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx())
                                    )
                                    
                                    val sweep = (reportsData.todayGlucoseAvg.coerceIn(0.0, 300.0) / 300.0 * 360.0).toFloat()
                                    val arcColor = if (reportsData.todayGlucoseAvg > profile.targetGlucoseMax || reportsData.todayGlucoseAvg < profile.targetGlucoseMin) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                    
                                    // 2. Base colored 3D tube (thick)
                                    drawArc(
                                        color = arcColor.copy(alpha = 0.4f),
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                    // 3. Core colored arc
                                    drawArc(
                                        color = arcColor,
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                    // 4. White light reflection highlight on the 3D tube
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.45f),
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val avgText = if (reportsData.todayGlucoseAvg > 0) String.format(Locale.getDefault(), "%.0f", reportsData.todayGlucoseAvg) else "N/A"
                                    Text(avgText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(profile.glucoseUnit, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Avg Glucose", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                        }

                        Divider(
                            modifier = Modifier
                                .height(50.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        // Total Insulin Dosage Ring
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(70.dp)) {
                                    // 1. Recessed background track
                                    drawCircle(
                                        color = Color.Black.copy(alpha = 0.18f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 7.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.05f),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx())
                                    )
                                    
                                    val sweep = (reportsData.todayInsulinTotal.coerceIn(0.0, 100.0) / 100.0 * 360.0).toFloat()
                                    val arcColor = Color(0xFF2196F3)
                                    
                                    // 2. Base colored 3D tube (thick)
                                    drawArc(
                                        color = arcColor.copy(alpha = 0.4f),
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                    // 3. Core colored arc
                                    drawArc(
                                        color = arcColor,
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                    // 4. White light reflection highlight on the 3D tube
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.45f),
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(String.format(Locale.getDefault(), "%.1f", reportsData.todayInsulinTotal), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("Units", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Glucose Trend Sparkline Chart
                    if (activeFeed.isNotEmpty()) {
                        val chartReadings = remember(activeFeed) { activeFeed.sortedBy { it.dateTimeMillis } }
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Glucose Trend (${chartReadings.size} logs)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    "Target: ${profile.targetGlucoseMin.toInt()}-${profile.targetGlucoseMax.toInt()} ${profile.glucoseUnit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                            val targetMin = profile.targetGlucoseMin
                            val targetMax = profile.targetGlucoseMax
                            val glucoseUnit = profile.glucoseUnit
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(106.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.18f),
                                                Color.Black.copy(alpha = 0.25f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 14.dp, horizontal = 16.dp)
                            ) {
                                val textColor = onSurfaceColor.toArgb()
                                val textPaint = remember(textColor) {
                                    android.graphics.Paint().apply {
                                        color = textColor
                                        textSize = 24f
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                }
                                val labelColor = onSurfaceColor.copy(alpha = 0.5f).toArgb()
                                val labelPaint = remember(labelColor) {
                                    android.graphics.Paint().apply {
                                        color = labelColor
                                        textSize = 18f
                                        textAlign = android.graphics.Paint.Align.LEFT
                                    }
                                }

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val width = size.width
                                    val height = size.height
                                    
                                    val minVal = (chartReadings.minOfOrNull { it.readingValue } ?: 70.0)
                                        .coerceAtMost(targetMin - 20.0)
                                    val maxVal = (chartReadings.maxOfOrNull { it.readingValue } ?: 180.0)
                                        .coerceAtLeast(targetMax + 20.0)
                                    val range = maxVal - minVal
                                    
                                    val targetMinY = (1f - ((targetMin - minVal) / range).toFloat()) * height
                                    val targetMaxY = (1f - ((targetMax - minVal) / range).toFloat()) * height
                                    
                                    // Target Range background band
                                    drawRect(
                                        color = Color(0xFF4CAF50).copy(alpha = 0.04f),
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, targetMaxY),
                                        size = androidx.compose.ui.geometry.Size(width, targetMinY - targetMaxY)
                                    )
                                    
                                    val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 10.dp.toPx()), 0f)
                                    // Dashed bounds
                                    drawLine(
                                        color = Color(0xFF4CAF50).copy(alpha = 0.25f),
                                        start = androidx.compose.ui.geometry.Offset(0f, targetMaxY),
                                        end = androidx.compose.ui.geometry.Offset(width, targetMaxY),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = dashEffect
                                    )
                                    drawLine(
                                        color = Color(0xFF4CAF50).copy(alpha = 0.25f),
                                        start = androidx.compose.ui.geometry.Offset(0f, targetMinY),
                                        end = androidx.compose.ui.geometry.Offset(width, targetMinY),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = dashEffect
                                    )
                                    
                                    // Label values
                                    drawContext.canvas.nativeCanvas.drawText(
                                        "Max Target: ${targetMax.toInt()}",
                                        10f,
                                        targetMaxY - 4.dp.toPx(),
                                        labelPaint
                                    )
                                    drawContext.canvas.nativeCanvas.drawText(
                                        "Min Target: ${targetMin.toInt()}",
                                        10f,
                                        targetMinY + 12.dp.toPx(),
                                        labelPaint
                                    )
                                    
                                    // Map points
                                    val chartCount = chartReadings.size
                                    val points = chartReadings.mapIndexed { index, reading ->
                                        val x = if (chartCount > 1) {
                                            index.toFloat() / (chartCount - 1) * width
                                        } else {
                                            width / 2f
                                        }
                                        val y = (1f - ((reading.readingValue - minVal) / range).toFloat()) * height
                                        Pair(x, y)
                                    }
                                    
                                    if (points.isNotEmpty()) {
                                        // Draw bezier line
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(points[0].first, points[0].second)
                                            for (i in 1 until points.size) {
                                                val p0 = points[i - 1]
                                                val p1 = points[i]
                                                val controlPointX1 = p0.first + (p1.first - p0.first) / 2
                                                val controlPointY1 = p0.second
                                                val controlPointX2 = p0.first + (p1.first - p0.first) / 2
                                                val controlPointY2 = p1.second
                                                cubicTo(
                                                    controlPointX1, controlPointY1,
                                                    controlPointX2, controlPointY2,
                                                    p1.first, p1.second
                                                )
                                            }
                                        }
                                        
                                        // Area gradient fill
                                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                                            addPath(path)
                                            lineTo(points.last().first, height)
                                            lineTo(points.first().first, height)
                                            close()
                                        }
                                        
                                        drawPath(
                                            path = fillPath,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    primaryColor.copy(alpha = 0.15f),
                                                    Color.Transparent
                                                ),
                                                startY = 0f,
                                                endY = height
                                            )
                                        )
                                        
                                        drawPath(
                                            path = path,
                                            color = primaryColor,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 3.dp.toPx(),
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        )
                                        
                                        // Pre-calculate point dimensions to avoid Composable/Density context issues inside lambda
                                        val radius1 = 7.dp.toPx()
                                        val radius2 = 4.dp.toPx()
                                        val radius3 = 2.5f.dp.toPx()
                                        val textOffset = 10.dp.toPx()

                                        // Points and labels
                                        points.forEachIndexed { idx, pt ->
                                            val reading = chartReadings[idx]
                                            val isOutOfRange = reading.readingValue < targetMin || reading.readingValue > targetMax
                                            val ptColor = if (isOutOfRange) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                            
                                            // Glowing circle pointer
                                            drawCircle(
                                                color = ptColor.copy(alpha = 0.25f),
                                                radius = radius1,
                                                center = androidx.compose.ui.geometry.Offset(pt.first, pt.second)
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = radius2,
                                                center = androidx.compose.ui.geometry.Offset(pt.first, pt.second)
                                            )
                                            drawCircle(
                                                color = ptColor,
                                                radius = radius3,
                                                center = androidx.compose.ui.geometry.Offset(pt.first, pt.second)
                                            )
                                            
                                            // Label value text
                                            val textVal = if (glucoseUnit == "mmol/L") {
                                                String.format(Locale.US, "%.1f", reading.readingValue)
                                            } else {
                                                reading.readingValue.toInt().toString()
                                            }
                                            
                                            drawContext.canvas.nativeCanvas.drawText(
                                                textVal,
                                                pt.first,
                                                pt.second - textOffset,
                                                textPaint
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 1. DYNAMIC RECENT GLUCOSE READINGS FEED
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = "Blood Sugar Readings List",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (todayGlucoseReadings.isNotEmpty()) "Recent Readings Today" else "Most Recent Readings",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (todayGlucoseReadings.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${todayGlucoseReadings.size} logged today",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        if (activeFeed.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .align(Alignment.CenterEnd)
                                    ) {
                                        val w = size.width
                                        val h = size.height
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(w * 0.65f, h * 0.5f)
                                            lineTo(w * 0.72f, h * 0.5f)
                                            lineTo(w * 0.75f, h * 0.15f)
                                            lineTo(w * 0.78f, h * 0.85f)
                                            lineTo(w * 0.81f, h * 0.5f)
                                            lineTo(w * 0.95f, h * 0.5f)
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(0xFFEF5350).copy(alpha = 0.5f),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 2.5f.dp.toPx(),
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = "No records",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "No glucose logs collected. Use quick actions to log blood sugar values.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        } else {
                            activeFeed.forEach { reading ->
                                val isOutOfRange = reading.readingValue < profile.targetGlucoseMin || reading.readingValue > profile.targetGlucoseMax
                                val bubbleColor = if (isOutOfRange) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                val textValue = if (profile.glucoseUnit == "mmol/L") reading.readingValue else reading.readingValue.toInt().toString()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(bubbleColor, CircleShape)
                                        )
                                        Column {
                                            Text(
                                                text = "${reading.mealContext} Log",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (reading.notes.isNotEmpty()) {
                                                Text(
                                                    text = reading.notes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$textValue ${profile.glucoseUnit}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOutOfRange) Color(0xFFFB8C00) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = timeFormatter.format(Date(reading.dateTimeMillis)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 2. DYNAMIC TODAY'S INSULIN DOSES FEED
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Vaccines,
                                    contentDescription = "Insulin Records",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Insulin Doses Today",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (todayInsulinRecords.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${todayInsulinRecords.size} taken today",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (todayInsulinRecords.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .align(Alignment.CenterEnd)
                                    ) {
                                        val w = size.width
                                        val h = size.height
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(w * 0.65f, h * 0.5f)
                                            lineTo(w * 0.72f, h * 0.5f)
                                            lineTo(w * 0.75f, h * 0.15f)
                                            lineTo(w * 0.78f, h * 0.85f)
                                            lineTo(w * 0.81f, h * 0.5f)
                                            lineTo(w * 0.95f, h * 0.5f)
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(0xFF2196F3).copy(alpha = 0.5f),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 2.5f.dp.toPx(),
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = "No insulin doses today",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "No insulin dosage logs collected for today. Log clinical doses above.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        } else {
                            todayInsulinRecords.forEach { record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                                .padding(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Vaccines,
                                                contentDescription = "Inject",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = record.insulinType,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (record.notes.isNotEmpty()) {
                                                Text(
                                                    text = record.notes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${record.doseUnits} Units",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = timeFormatter.format(Date(record.dateTimeMillis)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Active Reminders Highlights List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.navigateTo(AppScreen.REMINDERS) }) {
                    Text("View All", fontSize = 13.sp)
                }
            }
        }

        if (enabledReminders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.NotificationsOff, contentDescription = "No alerts", tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("No active reminders for today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            items(enabledReminders, key = { it.id }) { rem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (rem.reminderType == "Insulin") Icons.Default.Vaccines else Icons.Default.WaterDrop,
                                contentDescription = "Alert Type Icon",
                                tint = if (rem.reminderType == "Insulin") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(rem.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(rem.reminderType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Text(
                            text = viewModel.formatHourMinute(rem.hour, rem.minute),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // App Version Footer at bottom of Home Screen
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "GlucoLog Tracker v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Clinical Monitoring System",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            }
        }
    }

    QuickLogFab(
        onLogGlucose = onLogGlucoseClick,
        onLogInsulin = onLogInsulinClick,
        onLogBloodPressure = onLogBloodPressureClick,
        onLogSteps = onLogStepsClick,
        onRefillCartridge = onRefillClick,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    )
}

if (showProfileEditDialog) {
    ProfileEditDialog(
        currentProfile = profile,
        onDismiss = { showProfileEditDialog = false },
        onSave = { updatedProfile ->
            viewModel.saveProfile(updatedProfile)
            showProfileEditDialog = false
        }
    )
}
}
