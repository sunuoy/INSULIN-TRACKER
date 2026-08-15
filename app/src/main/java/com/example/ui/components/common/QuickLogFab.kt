package com.example.ui.components.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Unified Speed-Dial Floating Action Button (FAB)
 * Expands into quick-entry shortcuts for:
 *  - 🩸 Log Glucose
 *  - 💉 Log Insulin Dose
 *  - 💓 Log Blood Pressure
 *  - 👟 Log Steps
 *  - 🔄 Refill Cartridge
 */
@Composable
fun QuickLogFab(
    onLogGlucose: () -> Unit,
    onLogInsulin: () -> Unit,
    onLogBloodPressure: () -> Unit,
    onLogSteps: () -> Unit,
    onRefillCartridge: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fabRotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpeedDialItem(
                    label = "Log Glucose",
                    icon = Icons.Default.WaterDrop,
                    iconBg = Color(0xFFFB8C00),
                    onClick = {
                        expanded = false
                        onLogGlucose()
                    }
                )
                SpeedDialItem(
                    label = "Log Insulin Dose",
                    icon = Icons.Default.Vaccines,
                    iconBg = Color(0xFF3F51B5),
                    onClick = {
                        expanded = false
                        onLogInsulin()
                    }
                )
                SpeedDialItem(
                    label = "Log Blood Pressure",
                    icon = Icons.Default.Favorite,
                    iconBg = Color(0xFFE53935),
                    onClick = {
                        expanded = false
                        onLogBloodPressure()
                    }
                )
                SpeedDialItem(
                    label = "Log Steps",
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    iconBg = Color(0xFF10B981),
                    onClick = {
                        expanded = false
                        onLogSteps()
                    }
                )
                SpeedDialItem(
                    label = "Refill Cartridge",
                    icon = Icons.Default.Refresh,
                    iconBg = Color(0xFF00ACC1),
                    onClick = {
                        expanded = false
                        onRefillCartridge()
                    }
                )
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick Log Actions",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotationAngle)
            )
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Surface(
            shape = CircleShape,
            color = iconBg,
            shadowElevation = 4.dp,
            modifier = Modifier.size(42.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
