package com.example.ui.components.common

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
fun LoginLogo3D(isRegisterMode: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo3d")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(90.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                rotationZ = rotation
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        
        // 1. Draw 3D outer sphere shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = cx * 0.95f,
            center = androidx.compose.ui.geometry.Offset(cx + 2.dp.toPx(), cy + 4.dp.toPx())
        )
        
        // 2. Draw outer glowing ring with a nice gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color(0xFF312E81)),
                center = androidx.compose.ui.geometry.Offset(cx - w * 0.1f, cy - h * 0.1f),
                radius = cx * 0.9f
            ),
            radius = cx * 0.9f
        )
        
        // 3. Draw inner glassmorphic reflection overlay (specular reflection)
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.White.copy(alpha = 0.0f)
                ),
                startY = cy - cx * 0.85f,
                endY = cy
            ),
            radius = cx * 0.85f
        )
        
        // 4. Draw central 3D Shield or Heart representation
        val centerRadius = cx * 0.45f
        if (isRegisterMode) {
            // Register: 3D Person / ID badge icon
            // Draw head
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8)),
                    center = androidx.compose.ui.geometry.Offset(cx - centerRadius * 0.2f, cy - centerRadius * 0.4f),
                    radius = centerRadius * 0.4f
                ),
                radius = centerRadius * 0.35f,
                center = androidx.compose.ui.geometry.Offset(cx, cy - centerRadius * 0.35f)
            )
            // Draw body curve
            val bodyPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - centerRadius * 0.6f, cy + centerRadius * 0.6f)
                quadraticTo(cx, cy + centerRadius * 0.05f, cx + centerRadius * 0.6f, cy + centerRadius * 0.6f)
                quadraticTo(cx + centerRadius * 0.5f, cy + centerRadius * 0.85f, cx, cy + centerRadius * 0.85f)
                quadraticTo(cx - centerRadius * 0.5f, cy + centerRadius * 0.85f, cx - centerRadius * 0.6f, cy + centerRadius * 0.6f)
                close()
            }
            drawPath(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFCBD5E1), Color(0xFF64748B)),
                    center = androidx.compose.ui.geometry.Offset(cx, cy + centerRadius * 0.3f),
                    radius = centerRadius * 0.7f
                ),
                path = bodyPath
            )
        } else {
            // Login: 3D Secure Shield with a Lock inside
            val shieldPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - centerRadius * 0.8f)
                quadraticTo(cx + centerRadius * 0.8f, cy - centerRadius * 0.7f, cx + centerRadius * 0.8f, cy - centerRadius * 0.2f)
                quadraticTo(cx + centerRadius * 0.8f, cy + centerRadius * 0.4f, cx, cy + centerRadius * 0.9f)
                quadraticTo(cx - centerRadius * 0.8f, cy + centerRadius * 0.4f, cx - centerRadius * 0.8f, cy - centerRadius * 0.2f)
                quadraticTo(cx - centerRadius * 0.8f, cy - centerRadius * 0.7f, cx, cy - centerRadius * 0.8f)
                close()
            }
            drawPath(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFF854D0E)),
                    center = androidx.compose.ui.geometry.Offset(cx - centerRadius * 0.2f, cy - centerRadius * 0.3f),
                    radius = centerRadius * 0.9f
                ),
                path = shieldPath
            )
            
            // Draw shackle and lock body inside shield
            val lockBodyLeft = cx - centerRadius * 0.3f
            val lockBodyTop = cy
            val lockBodyW = centerRadius * 0.6f
            val lockBodyH = centerRadius * 0.45f
            
            drawRoundRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(cx - centerRadius * 0.2f, cy - centerRadius * 0.25f),
                size = androidx.compose.ui.geometry.Size(centerRadius * 0.4f, centerRadius * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(centerRadius * 0.2f, centerRadius * 0.2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                topLeft = androidx.compose.ui.geometry.Offset(lockBodyLeft, lockBodyTop),
                size = androidx.compose.ui.geometry.Size(lockBodyW, lockBodyH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF854D0E),
                radius = 2.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(cx, cy + lockBodyH * 0.4f)
            )
        }
    }
}


@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size / 2.0f
            val radius = size.minDimension / 2.0f
            
            drawArc(
                color = Color(0xFFEA4335), // Red
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = true
            )
            drawArc(
                color = Color(0xFFFBBC05), // Yellow
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = true
            )
            drawArc(
                color = Color(0xFF34A853), // Green
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = true
            )
            drawArc(
                color = Color(0xFF4285F4), // Blue
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = true
            )
        }
        Text(
            text = "G",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun AnimatedLowInsulinGraphic(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "insulinAnim")
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val translationY by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vibration"
    )

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .size(60.dp)
            .graphicsLayer {
                this.translationY = translationY
            }
    ) {
        val width = size.width
        val height = size.height
        
        drawCircle(
            color = Color.Red.copy(alpha = 0.08f * pulseAlpha),
            radius = width / 2f
        )
        
        val cartridgeWidth = 18.dp.toPx()
        val cartridgeHeight = 40.dp.toPx()
        val left = (width - cartridgeWidth) / 2f
        val top = (height - cartridgeHeight) / 2f
        
        drawRoundRect(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(cartridgeWidth, cartridgeHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.5.dp.toPx(), 4.5.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2.dp.toPx())
        )
        
        val pistonY = top + cartridgeHeight * 0.75f
        drawLine(
            color = Color.Gray,
            start = androidx.compose.ui.geometry.Offset(left + 3.dp.toPx(), pistonY),
            end = androidx.compose.ui.geometry.Offset(left + cartridgeWidth - 3.dp.toPx(), pistonY),
            strokeWidth = 3.dp.toPx()
        )
        
        val liquidHeight = top + cartridgeHeight - pistonY - 3.dp.toPx()
        if (liquidHeight > 0) {
            drawRoundRect(
                color = Color.Red.copy(alpha = pulseAlpha),
                topLeft = androidx.compose.ui.geometry.Offset(left + 3.dp.toPx(), pistonY + 1.5.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(cartridgeWidth - 6.dp.toPx(), liquidHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }
        
        val tipWidth = 5.dp.toPx()
        val tipHeight = 5.dp.toPx()
        drawRect(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = androidx.compose.ui.geometry.Offset((width - tipWidth) / 2f, top - tipHeight),
            size = androidx.compose.ui.geometry.Size(tipWidth, tipHeight)
        )
        
        val excX = width / 2f
        val excYStart = top + 7.dp.toPx()
        val excYEnd = top + 17.dp.toPx()
        
        drawLine(
            color = Color.Red,
            start = androidx.compose.ui.geometry.Offset(excX, excYStart),
            end = androidx.compose.ui.geometry.Offset(excX, excYEnd - 4.dp.toPx()),
            strokeWidth = 2.5.dp.toPx()
        )
        drawCircle(
            color = Color.Red,
            radius = 1.5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(excX, excYEnd)
        )
    }
}


@Composable
fun rememberRunningAnimation(isEnabled: Boolean): Modifier {
    if (!isEnabled) return Modifier
    val infiniteTransition = rememberInfiniteTransition(label = "runningAnim")
    val translationY by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translationY"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    val scaleY by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleY"
    )
    val scaleX by infiniteTransition.animateFloat(
        initialValue = 1.04f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleX"
    )
    return Modifier.graphicsLayer {
        this.translationY = translationY
        this.rotationZ = rotation
        this.scaleY = scaleY
        this.scaleX = scaleX
    }
}


@Composable
fun rememberBeatingHeartAnimation(isEnabled: Boolean): Modifier {
    if (!isEnabled) return Modifier
    val infiniteTransition = rememberInfiniteTransition(label = "heartBeatAnim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    return Modifier.graphicsLayer {
        this.scaleX = scale
        this.scaleY = scale
    }
}

fun playAdorableTone(toneName: String) {
    Thread {
        try {
            val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 85)
            when (toneName) {
                "Default" -> {
                    tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP)
                }
                "Gentle Chime" -> {
                    tg.startTone(android.media.ToneGenerator.TONE_DTMF_3, 80)
                    Thread.sleep(100)
                    tg.startTone(android.media.ToneGenerator.TONE_DTMF_6, 80)
                    Thread.sleep(100)
                    tg.startTone(android.media.ToneGenerator.TONE_DTMF_9, 120)
                }
                "Digital Alarm" -> {
                    tg.startTone(android.media.ToneGenerator.TONE_SUP_DIAL, 100)
                    Thread.sleep(180)
                    tg.startTone(android.media.ToneGenerator.TONE_SUP_DIAL, 100)
                }
                "Medical Alert" -> {
                    tg.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 60)
                    Thread.sleep(100)
                    tg.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 60)
                    Thread.sleep(100)
                    tg.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 80)
                }
                "Zen Harp" -> {
                    tg.startTone(android.media.ToneGenerator.TONE_DTMF_1, 60)
                    Thread.sleep(80)
                    tg.startTone(android.media.ToneGenerator.TONE_DTMF_5, 60)
                    Thread.sleep(80)
                    tg.startTone(android.media.ToneGenerator.TONE_DTMF_9, 60)
                    Thread.sleep(80)
                    tg.startTone(android.media.ToneGenerator.TONE_DTMF_A, 120)
                }
            }
            tg.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}


