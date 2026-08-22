package com.example.ui.screens.steps

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
fun StepsScreen(
    viewModel: com.example.ui.viewmodel.GlucoViewModel,
    onAddStepClick: () -> Unit,
    onEditStep: (com.example.data.model.StepCountRecord) -> Unit
) {
    val stepRecords by viewModel.stepRecords.collectAsStateWithLifecycle()
    val currentProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val liveSensorSteps by com.example.services.StepCounterService.liveStepsFlow.collectAsStateWithLifecycle()
    val isSensorActive by com.example.services.StepCounterService.isSensorActiveFlow.collectAsStateWithLifecycle()
    val activeSensorType by com.example.services.StepCounterService.activeSensorTypeFlow.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var isPermissionGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissions[android.Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        } else {
            true
        }
        if (isPermissionGranted) {
            try {
                val serviceIntent = android.content.Intent(context, com.example.services.StepCounterService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("StepsScreen", "Failed to start service on permission grant", e)
            }
        }
    }

    // Calculate today's steps in real-time
    val todayDbSteps = remember(stepRecords) {
        val calendar = java.util.Calendar.getInstance()
        val todayYear = calendar.get(java.util.Calendar.YEAR)
        val todayDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)

        stepRecords.filter { record ->
            val recCal = java.util.Calendar.getInstance().apply { timeInMillis = record.dateTimeMillis }
            recCal.get(java.util.Calendar.YEAR) == todayYear && recCal.get(java.util.Calendar.DAY_OF_YEAR) == todayDay
        }.sumOf { it.steps }
    }

    val todaySteps = maxOf(todayDbSteps, liveSensorSteps)

    val stepGoal = if (currentProfile.stepGoal > 0) currentProfile.stepGoal else 10000
    val progress = (todaySteps.toFloat() / stepGoal).let { 
        if (it.isNaN() || it.isInfinite()) 0f else it.coerceIn(0f, 1f) 
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddStepClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Steps")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission request banner if disabled
            if (!isPermissionGranted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "Pedometer",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto Step Tracking Disabled",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Enable phone sensors permission to automatically track your daily steps in real time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                            permissionLauncher.launch(
                                                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION)
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Enable Auto-Track", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Sensor Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSensorActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, if (isSensorActive) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (isSensorActive) Color(0xFF10B981) else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                            Column {
                                Text(
                                    text = if (isSensorActive) "Sensor: $activeSensorType" else "Sensor: Standby",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "0ms real-time cadence detection",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Material 3 "Today's Progress" & Biometrics Dashboard Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A), // Deep Slate Navy
                                        Color(0xFF1E293B)  // Dark Slate
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        // Ambient dynamic Canvas background
                        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = 0.07f),
                                radius = size.height * 0.55f,
                                center = Offset(size.width * 0.88f, size.height * 0.2f)
                            )
                            drawCircle(
                                color = Color(0xFF34D399).copy(alpha = 0.05f),
                                radius = size.height * 0.45f,
                                center = Offset(size.width * 0.12f, size.height * 0.8f)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Card Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Today's Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            val heightM = currentProfile.heightCm / 100.0
                            val bmi = if (heightM > 0) currentProfile.weightKg / (heightM * heightM) else 0.0
                            val (bmiCategory, bmiColor) = when {
                                bmi <= 0.0 -> "N/A" to Color.Gray
                                bmi < 18.5 -> "Underweight" to Color(0xFFFBBF24)
                                bmi < 25.0 -> "Normal" to Color(0xFF34D399)
                                bmi < 30.0 -> "Overweight" to Color(0xFFFB923C)
                                else -> "Obese" to Color(0xFFF87171)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Column: Height, Weight & BMI
                                Column(
                                    modifier = Modifier.weight(1.15f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Height & Weight row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Height Card
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Height",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Straighten,
                                                        contentDescription = null,
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${String.format(java.util.Locale.US, "%.1f", currentProfile.heightCm)} cm",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // Weight Card
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Weight",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Speed,
                                                        contentDescription = null,
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${String.format(java.util.Locale.US, "%.1f", currentProfile.weightKg)} kg",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    // BMI Card
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "BMI Index",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(1.dp))
                                                    Text(
                                                        text = if (bmi > 0) String.format(java.util.Locale.US, "%.1f", bmi) else "N/A",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = bmiColor
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(bmiColor.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                                                        .border(1.dp, bmiColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = bmiCategory,
                                                        color = bmiColor,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }

                                            // Modern Segmented BMI Bar
                                            if (bmi > 0) {
                                                androidx.compose.foundation.Canvas(
                                                    modifier = Modifier.fillMaxWidth().height(14.dp)
                                                ) {
                                                    val trackHeight = 6.dp.toPx()
                                                    val yCenter = size.height / 2f
                                                    val segmentSpacing = 3.dp.toPx()
                                                    
                                                    val totalWidth = size.width
                                                    val usableWidth = totalWidth - (3 * segmentSpacing)
                                                    val w1 = usableWidth * 0.175f
                                                    val w2 = usableWidth * 0.325f
                                                    val w3 = usableWidth * 0.25f
                                                    val w4 = usableWidth * 0.25f

                                                    // Segment 1: Underweight
                                                    drawRoundRect(
                                                        color = Color(0xFFFBBF24),
                                                        topLeft = Offset(0f, yCenter - trackHeight/2),
                                                        size = Size(w1, trackHeight),
                                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                                    )
                                                    // Segment 2: Normal
                                                    drawRoundRect(
                                                        color = Color(0xFF34D399),
                                                        topLeft = Offset(w1 + segmentSpacing, yCenter - trackHeight/2),
                                                        size = Size(w2, trackHeight),
                                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                                    )
                                                    // Segment 3: Overweight
                                                    drawRoundRect(
                                                        color = Color(0xFFFB923C),
                                                        topLeft = Offset(w1 + w2 + 2 * segmentSpacing, yCenter - trackHeight/2),
                                                        size = Size(w3, trackHeight),
                                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                                    )
                                                    // Segment 4: Obese
                                                    drawRoundRect(
                                                        color = Color(0xFFF87171),
                                                        topLeft = Offset(w1 + w2 + w3 + 3 * segmentSpacing, yCenter - trackHeight/2),
                                                        size = Size(w4, trackHeight),
                                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                                    )

                                                    val bmiClamped = bmi.toFloat().coerceIn(15f, 35f)
                                                    val pct = (bmiClamped - 15f) / (35f - 15f)
                                                    
                                                    val pointerX = when {
                                                        pct <= 0.175f -> (pct / 0.175f) * w1
                                                        pct <= 0.500f -> w1 + segmentSpacing + (((pct - 0.175f) / 0.325f) * w2)
                                                        pct <= 0.750f -> w1 + w2 + 2 * segmentSpacing + (((pct - 0.500f) / 0.25f) * w3)
                                                        else -> w1 + w2 + w3 + 3 * segmentSpacing + (((pct - 0.750f) / 0.25f) * w4)
                                                    }

                                                    drawCircle(
                                                        color = Color.White,
                                                        radius = 6.5f.dp.toPx(),
                                                        center = Offset(pointerX, yCenter)
                                                    )
                                                    drawCircle(
                                                        color = bmiColor,
                                                        radius = 4.dp.toPx(),
                                                        center = Offset(pointerX, yCenter)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Right Column: Circular Step Progress Ring
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(0.95f)
                                ) {
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = progress.coerceIn(0f, 1f),
                                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                        label = "stepsProgressAnim"
                                    )

                                    val haloTransition = rememberInfiniteTransition(label = "haloAnim")
                                    val haloRotation by haloTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(12000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "haloRotation"
                                    )

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(130.dp)
                                    ) {
                                        // 1. Rotating dashed ambient halo
                                        androidx.compose.foundation.Canvas(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .graphicsLayer { rotationZ = haloRotation }
                                        ) {
                                            drawCircle(
                                                color = if (progress >= 1f) Color(0xFF34D399).copy(alpha = 0.35f) else Color(0xFF38BDF8).copy(alpha = 0.3f),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = 1.5.dp.toPx(),
                                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                                                )
                                            )
                                        }

                                        // 2. Custom Arc Progress Ring with Gradient Sweep
                                        val isGoalReached = progress >= 1f
                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                            val strokeWidth = 10.dp.toPx()
                                            val diameter = size.minDimension - strokeWidth
                                            val topLeftOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)
                                            val arcSize = Size(diameter, diameter)
                                            val radius = diameter / 2f
                                            val centerPt = Offset(size.width / 2f, size.height / 2f)

                                            // Draw track background
                                            drawArc(
                                                color = Color.White.copy(alpha = 0.08f),
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                topLeft = topLeftOffset,
                                                size = arcSize,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                            )

                                            // Draw animated progress arc
                                            if (animatedProgress > 0.001f) {
                                                val sweepAngle = animatedProgress * 360f
                                                val gradientColors = if (isGoalReached) {
                                                    listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF059669), Color(0xFF34D399))
                                                } else {
                                                    listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9), Color(0xFF2DD4BF), Color(0xFF38BDF8))
                                                }
                                                val brush = Brush.sweepGradient(gradientColors, center = centerPt)

                                                drawArc(
                                                    brush = brush,
                                                    startAngle = -90f,
                                                    sweepAngle = sweepAngle,
                                                    useCenter = false,
                                                    topLeft = topLeftOffset,
                                                    size = arcSize,
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                )

                                                // Glowing indicator orb at tip
                                                val endAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
                                                val orbX = centerPt.x + radius * Math.cos(endAngleRad).toFloat()
                                                val orbY = centerPt.y + radius * Math.sin(endAngleRad).toFloat()

                                                drawCircle(
                                                    color = if (isGoalReached) Color(0xFF34D399).copy(alpha = 0.6f) else Color(0xFF38BDF8).copy(alpha = 0.6f),
                                                    radius = strokeWidth * 0.9f,
                                                    center = Offset(orbX, orbY)
                                                )
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = strokeWidth * 0.45f,
                                                    center = Offset(orbX, orbY)
                                                )
                                            }
                                        }

                                        // 3. Center metric stack with animated walk icon
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsWalk,
                                                contentDescription = "Walk",
                                                tint = if (isGoalReached) Color(0xFF34D399) else Color(0xFF38BDF8),
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .then(rememberRunningAnimation(isEnabled = isPermissionGranted))
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = String.format(java.util.Locale.US, "%,d", todaySteps),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${(progress * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGoalReached) Color(0xFF34D399) else Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (progress >= 1f) "Goal achieved! 🎉" else "${((1f - progress) * stepGoal).toInt()} steps left",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progress >= 1f) Color(0xFF34D399) else Color.White.copy(alpha = 0.65f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Metrics Row: Material 3 Elevated Distance & Calories Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Distance Card
                    val estKm = todaySteps * 0.00075
                    val kmGoal = 5.0
                    val kmProgress = (estKm / kmGoal).coerceIn(0.0, 1.0).toFloat()
                    val animKmProgress by animateFloatAsState(
                        targetValue = kmProgress,
                        animationSpec = tween(1000, easing = FastOutSlowInEasing),
                        label = "kmProgressAnim"
                    )

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Distance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsRun,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp).then(rememberRunningAnimation(isEnabled = isPermissionGranted))
                                    )
                                }
                            }
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f km", estKm),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LinearProgressIndicator(
                                progress = { animKmProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF10B981).copy(alpha = 0.15f)
                            )
                            Text(
                                text = "${(kmProgress * 100).toInt()}% of 5.0 km goal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Calories Card
                    val estKcal = todaySteps * 0.04
                    val kcalGoal = 400.0
                    val kcalProgress = (estKcal / kcalGoal).coerceIn(0.0, 1.0).toFloat()
                    val animKcalProgress by animateFloatAsState(
                        targetValue = kcalProgress,
                        animationSpec = tween(1000, easing = FastOutSlowInEasing),
                        label = "kcalProgressAnim"
                    )

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Calories",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFF43F5E).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${estKcal.toInt()} kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LinearProgressIndicator(
                                progress = { animKcalProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFF43F5E),
                                trackColor = Color(0xFFF43F5E).copy(alpha = 0.15f)
                            )
                            Text(
                                text = "${(kcalProgress * 100).toInt()}% of 400 kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Steps Log Header — with decorative accent
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(22.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF4FC3F7), Color(0xFF1E88E5))
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    Text(
                        text = "Steps Logs History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (stepRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No step logs recorded.\nClick '+' to log your walking activity.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(stepRecords) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Colorful left accent bar
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF4FC3F7), Color(0xFF1E88E5))
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier.weight(1f).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    Color(0xFF4FC3F7).copy(alpha = 0.12f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsWalk,
                                                contentDescription = null,
                                                tint = Color(0xFF4FC3F7),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = String.format("%,d steps", record.steps),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = viewModel.formatEpochToDate(record.dateTimeMillis),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    if (record.notes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = record.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = { onEditStep(record) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline)
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteStepRecord(record)
                                        android.widget.Toast.makeText(context, "Step log deleted", android.widget.Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

