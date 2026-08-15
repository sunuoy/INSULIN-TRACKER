package com.example.ui.components.dialogs

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
    onSaveAsNew: ((UserProfile) -> Unit)? = null,
    loggedInUser: String = "",
    isAdmin: Boolean = false,
    onLogout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gluco_auth_prefs", android.content.Context.MODE_PRIVATE) }
    val linkedEmail = remember(loggedInUser) { prefs.getString("user_email_$loggedInUser", "") ?: "" }

    var uName by remember { mutableStateOf(currentProfile.userName) }
    var dName by remember { mutableStateOf(currentProfile.doctorName) }
    var dMail by remember { mutableStateOf(currentProfile.doctorEmail) }
    var dPhone by remember { mutableStateOf(currentProfile.doctorPhone) }
    var tMin by remember { mutableStateOf(currentProfile.targetGlucoseMin.toString()) }
    var tMax by remember { mutableStateOf(currentProfile.targetGlucoseMax.toString()) }
    var gUnit by remember { mutableStateOf(currentProfile.glucoseUnit) }
    var stepGoalInput by remember { mutableStateOf(currentProfile.stepGoal.toString()) }
    var heightInput by remember { mutableStateOf(currentProfile.heightCm.toString()) }
    var weightInput by remember { mutableStateOf(currentProfile.weightKg.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("profile_edit_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit Active Credentials",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (onLogout != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_auth_card_in_settings"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Logged in as:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = loggedInUser.ifEmpty { "Guest Patient" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (isAdmin) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "ADMIN",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (linkedEmail.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Verified Email",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = linkedEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = onLogout,
                                modifier = Modifier.testTag("logout_button_settings"),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Sign Out Icon",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Out", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Patient and Doctor names side-by-side! (Double-column)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uName,
                        onValueChange = { uName = it },
                        label = { Text("Patient Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dName,
                        onValueChange = { dName = it },
                        label = { Text("Doctor Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Doctor email and Doctor Phone side-by-side! (Double-column)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dMail,
                        onValueChange = { dMail = it },
                        label = { Text("Doctor Email") },
                        modifier = Modifier.weight(1.1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dPhone,
                        onValueChange = { dPhone = it },
                        label = { Text("Doctor Phone") },
                        modifier = Modifier.weight(0.9f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Low and High limits + Unit selector side-by-side! (Triple-column)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tMin,
                        onValueChange = { tMin = it },
                        label = { Text("Low Limit") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tMax,
                        onValueChange = { tMax = it },
                        label = { Text("High Limit") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Segmented Control for Units (mg/dL vs mmol/L)
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            "Unit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            val units = listOf("mg/dL", "mmol/L")
                            units.forEach { unit ->
                                val isSelected = gUnit == unit
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { gUnit = unit },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unit,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Step Goal, Height, Weight inputs row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stepGoalInput,
                        onValueChange = { stepGoalInput = it },
                        label = { Text("Step Goal") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.weight(0.1f))

                    if (onSaveAsNew != null) {
                        OutlinedButton(
                            modifier = Modifier
                                .weight(2f)
                                .height(46.dp)
                                .testTag("profile_save_as_new_button"),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                val pMin = tMin.toDoubleOrNull() ?: 70.0
                                val pMax = tMax.toDoubleOrNull() ?: 140.0
                                val pSteps = stepGoalInput.toIntOrNull() ?: 10000
                                val pHeight = heightInput.toDoubleOrNull() ?: 170.0
                                val pWeight = weightInput.toDoubleOrNull() ?: 70.0
                                onSaveAsNew(
                                    UserProfile(
                                        id = 0,
                                        userName = uName.ifEmpty { "Patient" },
                                        doctorName = dName,
                                        doctorEmail = dMail,
                                        doctorPhone = dPhone,
                                        targetGlucoseMin = pMin,
                                        targetGlucoseMax = pMax,
                                        glucoseUnit = gUnit,
                                        isActive = true,
                                        stepGoal = pSteps,
                                        heightCm = pHeight,
                                        weightKg = pWeight
                                    )
                                )
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New Profile Logo",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save as New", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    Button(
                        modifier = Modifier
                            .weight(2f)
                            .height(46.dp)
                            .testTag("profile_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            val pMin = tMin.toDoubleOrNull() ?: 70.0
                            val pMax = tMax.toDoubleOrNull() ?: 140.0
                            val pSteps = stepGoalInput.toIntOrNull() ?: 10000
                            val pHeight = heightInput.toDoubleOrNull() ?: 170.0
                            val pWeight = weightInput.toDoubleOrNull() ?: 70.0
                            onSave(
                                currentProfile.copy(
                                    userName = uName.ifEmpty { "Patient" },
                                    doctorName = dName,
                                    doctorEmail = dMail,
                                    doctorPhone = dPhone,
                                    targetGlucoseMin = pMin,
                                    targetGlucoseMax = pMax,
                                    glucoseUnit = gUnit,
                                    stepGoal = pSteps,
                                    heightCm = pHeight,
                                    weightKg = pWeight
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save Profile Logo",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (onSaveAsNew != null) "Save Active" else "Save Profile", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

