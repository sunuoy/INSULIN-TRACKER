package com.example.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BloodPressureRecord
import com.example.data.model.GlucoseReading
import com.example.data.model.UserProfile

/**
 * High-visibility clinical warning alert banners for critical states:
 *  - Hypoglycemia (< 70 mg/dL): Urgent Fast Carbs rule
 *  - Severe Hyperglycemia (> 250 mg/dL): Hydration & Ketone check
 *  - Hypertensive Crisis (> 140 / 90 mmHg): Blood pressure alert
 */
@Composable
fun VitalAlertBannersSection(
    latestGlucose: GlucoseReading?,
    latestBp: BloodPressureRecord?,
    profile: UserProfile,
    onTakeActionClick: () -> Unit = {}
) {
    var dismissedHypo by remember { mutableStateOf(false) }
    var dismissedHyper by remember { mutableStateOf(false) }
    var dismissedBp by remember { mutableStateOf(false) }

    val targetMin = profile.targetGlucoseMin
    val targetMax = profile.targetGlucoseMax
    val isMmol = profile.glucoseUnit == "mmol/L"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Hypoglycemia Alert (< 70 mg/dL or < 3.9 mmol/L)
        if (latestGlucose != null && latestGlucose.readingValue < targetMin && !dismissedHypo) {
            val isSevere = latestGlucose.readingValue < (if (isMmol) 3.0 else 55.0)
            VitalBanner(
                title = if (isSevere) "CRITICAL: Severe Hypoglycemia" else "Low Glucose Alert (< ${targetMin.toInt()} ${profile.glucoseUnit})",
                message = "Latest reading: ${latestGlucose.readingValue} ${profile.glucoseUnit}. Take 15g of fast-acting carbohydrates (juice/glucose tabs) and re-test in 15 minutes.",
                gradientColors = listOf(Color(0xFFDC2626), Color(0xFF991B1B)),
                icon = Icons.Default.Emergency,
                onDismiss = { dismissedHypo = true }
            )
        }

        // 2. Severe Hyperglycemia Alert (> 240 mg/dL or > 13.3 mmol/L)
        val hyperThreshold = if (isMmol) 13.3 else 240.0
        if (latestGlucose != null && latestGlucose.readingValue >= hyperThreshold && !dismissedHyper) {
            VitalBanner(
                title = "High Glucose Warning (${latestGlucose.readingValue} ${profile.glucoseUnit})",
                message = "Blood sugar is elevated above normal thresholds. Drink plenty of water and verify active insulin on board (IOB) before correctional doses.",
                gradientColors = listOf(Color(0xFFEA580C), Color(0xFFC2410C)),
                icon = Icons.Default.Warning,
                onDismiss = { dismissedHyper = true }
            )
        }

        // 3. High Blood Pressure Stage 2 Alert (Systolic >= 140 or Diastolic >= 90)
        if (latestBp != null && (latestBp.systolic >= 140 || latestBp.diastolic >= 90) && !dismissedBp) {
            VitalBanner(
                title = "Elevated Blood Pressure (${latestBp.systolic}/${latestBp.diastolic} mmHg)",
                message = "Systolic or diastolic reading is in the Stage 2 hypertensive range. Rest for 5 minutes in a calm position and re-check.",
                gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)),
                icon = Icons.Default.Favorite,
                onDismiss = { dismissedBp = true }
            )
        }
    }
}

@Composable
private fun VitalBanner(
    title: String,
    message: String,
    gradientColors: List<Color>,
    icon: ImageVector,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 16.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Alert",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
