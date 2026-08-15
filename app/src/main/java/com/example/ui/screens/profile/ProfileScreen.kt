package com.example.ui.screens.profile

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
fun ProfileScreen(viewModel: GlucoViewModel) {
    val currentProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val savedProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gluco_auth_prefs", android.content.Context.MODE_PRIVATE) }

    var uName by remember(currentProfile) { mutableStateOf(currentProfile.userName) }
    var dName by remember(currentProfile) { mutableStateOf(currentProfile.doctorName) }
    var dMail by remember(currentProfile) { mutableStateOf(currentProfile.doctorEmail) }
    var dPhone by remember(currentProfile) { mutableStateOf(currentProfile.doctorPhone) }
    var tMin by remember(currentProfile) { mutableStateOf(currentProfile.targetGlucoseMin.toString()) }
    var tMax by remember(currentProfile) { mutableStateOf(currentProfile.targetGlucoseMax.toString()) }
    var gUnit by remember(currentProfile) { mutableStateOf(currentProfile.glucoseUnit) }
    var stepGoalInput by remember(currentProfile) { mutableStateOf(currentProfile.stepGoal.toString()) }
    var heightInput by remember(currentProfile) { mutableStateOf(currentProfile.heightCm.toString()) }
    var weightInput by remember(currentProfile) { mutableStateOf(currentProfile.weightKg.toString()) }

    var selectedAvatarIndex by remember(currentProfile.id) {
        mutableStateOf(prefs.getInt("profile_avatar_${currentProfile.id}", 0))
    }

    val avatarOptions = listOf(
        Icons.Default.MedicalServices to Color(0xFF1E88E5), // Blue
        Icons.Default.Person to Color(0xFF8E24AA),          // Purple
        Icons.Default.Favorite to Color(0xFFE53935),        // Red
        Icons.Default.CloudQueue to Color(0xFF00ACC1),      // Teal cyan
        Icons.Default.HealthAndSafety to Color(0xFF43A047), // Green
        Icons.Default.LocalHospital to Color(0xFFFB8C00)    // Orange
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Headline
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Clinical Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Quick-switch and edit profiles compactly",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Horizontal Saved Profiles List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Saved Profiles List",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                if (savedProfiles.isEmpty()) {
                    Text(
                        text = "No saved profiles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(savedProfiles, key = { it.id }) { profile ->
                            val isActive = profile.isActive || profile.id == currentProfile.id
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

                            Card(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(108.dp)
                                    .clickable { viewModel.selectProfileFlow(profile.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isActive) 2.dp else 1.dp,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Small circular avatar
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(avatarColor, androidx.compose.foundation.shape.CircleShape)
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = avatarIcon,
                                                contentDescription = "Avatar Icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Patient Name
                                            Text(
                                                text = profile.userName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                            )
                                            // Doctor Info
                                            Text(
                                                text = "Dr: ${profile.doctorName.ifEmpty { "None" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                                            )
                                            // Limits
                                            Text(
                                                text = "Range: ${profile.targetGlucoseMin.toInt()}-${profile.targetGlucoseMax.toInt()} ${profile.glucoseUnit}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    // Active indicator or Delete button
                                    Row(
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { viewModel.deleteProfileFlow(profile) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Profile",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
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
        }

        // Profile Picture Circle & Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose Profile Picture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Large circular display with currently active avatar!
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = avatarOptions.getOrNull(selectedAvatarIndex)?.second ?: Color.Gray,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarOptions.getOrNull(selectedAvatarIndex)?.first ?: Icons.Default.Person,
                            contentDescription = "Active Avatar Icon",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Row of selectable options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        avatarOptions.forEachIndexed { index, (icon, color) ->
                            val isSelected = index == selectedAvatarIndex
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(color, androidx.compose.foundation.shape.CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable {
                                        selectedAvatarIndex = index
                                        prefs.edit().putInt("profile_avatar_${currentProfile.id}", index).apply()
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Avatar Choice $index",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edit Active Credentials Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Edit Active Credentials",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

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

                    Spacer(modifier = Modifier.height(6.dp))

                    // Buttons Group (Save Changes & Save as New Profile)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Outlined Save as New
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                val pMin = tMin.toDoubleOrNull() ?: 70.0
                                val pMax = tMax.toDoubleOrNull() ?: 140.0
                                viewModel.saveNewProfileFlow(
                                    com.example.data.model.UserProfile(
                                        id = 0,
                                        userName = uName.ifEmpty { "Patient" },
                                        doctorName = dName,
                                        doctorEmail = dMail,
                                        doctorPhone = dPhone,
                                        targetGlucoseMin = pMin,
                                        targetGlucoseMax = pMax,
                                        glucoseUnit = gUnit,
                                        isActive = true
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
                            Text("Save as New", fontSize = 12.sp, maxLines = 1)
                        }

                        // Filled Save Changes
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("profile_save_button_clinical_profile"),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                val pMin = tMin.toDoubleOrNull() ?: 70.0
                                val pMax = tMax.toDoubleOrNull() ?: 140.0
                                viewModel.saveProfile(
                                    com.example.data.model.UserProfile(
                                        id = currentProfile.id,
                                        userName = uName.ifEmpty { "Patient" },
                                        doctorName = dName,
                                        doctorEmail = dMail,
                                        doctorPhone = dPhone,
                                        targetGlucoseMin = pMin,
                                        targetGlucoseMax = pMax,
                                        glucoseUnit = gUnit,
                                        isActive = true,
                                        cartridgeCapacity = currentProfile.cartridgeCapacity,
                                        cartridgeRemaining = currentProfile.cartridgeRemaining
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
                            Text("Save Active", fontSize = 12.sp, maxLines = 1)
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

                    // Buttons Group (Save Changes & Save as New Profile)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Outlined Save as New
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                val pMin = tMin.toDoubleOrNull() ?: 70.0
                                val pMax = tMax.toDoubleOrNull() ?: 140.0
                                val pSteps = stepGoalInput.toIntOrNull() ?: 10000
                                val pHeight = heightInput.toDoubleOrNull() ?: 170.0
                                val pWeight = weightInput.toDoubleOrNull() ?: 70.0
                                viewModel.saveNewProfileFlow(
                                    com.example.data.model.UserProfile(
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
                            Text("Save as New", fontSize = 12.sp, maxLines = 1)
                        }

                        // Filled Save Changes
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("profile_save_button_clinical_profile"),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                val pMin = tMin.toDoubleOrNull() ?: 70.0
                                val pMax = tMax.toDoubleOrNull() ?: 140.0
                                val pSteps = stepGoalInput.toIntOrNull() ?: 10000
                                val pHeight = heightInput.toDoubleOrNull() ?: 170.0
                                val pWeight = weightInput.toDoubleOrNull() ?: 70.0
                                viewModel.saveProfile(
                                    com.example.data.model.UserProfile(
                                        id = currentProfile.id,
                                        userName = uName.ifEmpty { "Patient" },
                                        doctorName = dName,
                                        doctorEmail = dMail,
                                        doctorPhone = dPhone,
                                        targetGlucoseMin = pMin,
                                        targetGlucoseMax = pMax,
                                        glucoseUnit = gUnit,
                                        isActive = true,
                                        cartridgeCapacity = currentProfile.cartridgeCapacity,
                                        cartridgeRemaining = currentProfile.cartridgeRemaining,
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
                            Text("Save Active", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // BMI & Fitness Health Summary Card
        item {
            val heightM = currentProfile.heightCm / 100.0
            val bmi = if (heightM > 0) currentProfile.weightKg / (heightM * heightM) else 0.0
            val (bmiCategory, bmiColor) = when {
                bmi <= 0.0 -> "N/A" to Color.Gray
                bmi < 18.5 -> "Underweight" to Color(0xFFFBC02D) // Yellow
                bmi < 25.0 -> "Normal Weight" to Color(0xFF4CAF50) // Green
                bmi < 30.0 -> "Overweight" to Color(0xFFF57C00) // Orange
                else -> "Obese" to Color(0xFFD32F2F) // Red
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BMI & Health Summary",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .background(bmiColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = bmiCategory,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Estimated BMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = if (bmi > 0) String.format("%.1f kg/m²", bmi) else "N/A",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Height:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text("${currentProfile.heightCm} cm", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Weight:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text("${currentProfile.weightKg} kg", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Step Goal:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(String.format("%,d", currentProfile.stepGoal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Simulated System Clock Dashboard
        item {
            val customOffset by viewModel.customTimeOffsetMillis.collectAsStateWithLifecycle()
            var currentVirtualTime by remember { mutableStateOf(viewModel.getCurrentTimeMillis()) }
            var showSetTimeDialog by remember { mutableStateOf(false) }

            LaunchedEffect(customOffset) {
                while (true) {
                    currentVirtualTime = viewModel.getCurrentTimeMillis()
                    delay(1000L)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("simulated_time_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Simulated Time Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Simulated System Clock",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        if (customOffset != 0L) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Offset Active",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "Current App Time: ${viewModel.formatEpochToDate(currentVirtualTime)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (customOffset != 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Shift or fast-forward the application's clock to simulate future or past events (e.g., checking medication log history or reminder schedules).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Presets
                    Text(
                        text = "Preset Time Shifters:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.setSystemTime(viewModel.getCurrentTimeMillis() + 3600000L) }, // +1 Hour
                            label = { Text("+1 Hour") }
                        )
                        AssistChip(
                            onClick = { viewModel.setSystemTime(viewModel.getCurrentTimeMillis() + 10800000L) }, // +3 Hours
                            label = { Text("+3 Hours") }
                        )
                        AssistChip(
                            onClick = { viewModel.setSystemTime(viewModel.getCurrentTimeMillis() + 86400000L) }, // +1 Day
                            label = { Text("+1 Day") }
                        )
                        AssistChip(
                            onClick = { viewModel.setSystemTime(viewModel.getCurrentTimeMillis() + 604800000L) }, // +7 Days
                            label = { Text("+7 Days") }
                        )
                        AssistChip(
                            onClick = { viewModel.setSystemTime(viewModel.getCurrentTimeMillis() - 86400000L) }, // -1 Day
                            label = { Text("-1 Day") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetSystemTime() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = customOffset != 0L
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Clock")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset clock", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showSetTimeDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Specify Time")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Custom...", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (showSetTimeDialog) {
                var inputDate by remember { mutableStateOf(viewModel.formatEpochToDateOnly(viewModel.getCurrentTimeMillis())) }
                var inputTime by remember { mutableStateOf(viewModel.formatEpochToTimeOnly(viewModel.getCurrentTimeMillis())) }

                Dialog(onDismissRequest = { showSetTimeDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Specify Custom Date & Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = inputDate,
                                onValueChange = { inputDate = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = inputTime,
                                onValueChange = { inputTime = it },
                                label = { Text("Time (HH:MM)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showSetTimeDialog = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val calendar = viewModel.composeCalendarFromDateStrAndTimeStr(inputDate, inputTime)
                                        viewModel.setSystemTime(calendar.timeInMillis)
                                        showSetTimeDialog = false
                                    }
                                ) {
                                    Text("Apply")
                                }
                            }
                        }
                    }
                }
            }
        }


        


        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// FORM DIALOGS (CONTAINED MODALS)
// ==========================================
