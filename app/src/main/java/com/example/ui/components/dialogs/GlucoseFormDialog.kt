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
fun GlucoseFormDialog(
    viewModel: GlucoViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isMmolVal = profile.glucoseUnit == "mmol/L"

    var value by remember { mutableStateOf(viewModel.glucValue) }
    var context by remember { mutableStateOf(viewModel.glucMealContext.ifEmpty { "Fasting" }) }
    var date by remember { mutableStateOf(viewModel.glucDate.ifEmpty { viewModel.formatEpochToDateOnly(viewModel.getCurrentTimeMillis()) }) }
    var time by remember { mutableStateOf(viewModel.glucTime.ifEmpty { viewModel.formatEpochToTimeOnly(viewModel.getCurrentTimeMillis()) }) }
    var notes by remember { mutableStateOf(viewModel.glucNotes) }

    val (isError, errorMessage) = remember(value, isMmolVal) {
        val trimmed = value.trim()
        val num = trimmed.toDoubleOrNull()
        when {
            trimmed.isEmpty() -> {
                true to "Glucose reading cannot be empty"
            }
            num == null -> {
                true to "Please enter a valid numeric level"
            }
            isMmolVal && (num < 1.1 || num > 35.0) -> {
                true to "Realistic range for mmol/L is 1.1 - 35.0"
            }
            !isMmolVal && (num < 20.0 || num > 600.0) -> {
                true to "Realistic range for mg/dL is 20 - 600"
            }
            else -> {
                false to ""
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("glucose_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Glucose Sugar Icon",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (viewModel.selectedGlucoseIdToEdit != null) "Edit Glucose Reading" else "Add Glucose Reading",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 1. GLUCOSE LEVEL VALUE INPUT CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Blood Sugar Level",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text("Glucose Level Reading") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("glucose_value_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            isError = isError,
                            supportingText = {
                                if (isError) {
                                    Text(
                                        text = errorMessage,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "Unit type: ${profile.glucoseUnit} (Realistic range)",
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                // 2. MEAL CONTEXT CHIPS CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Meal Context Check",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Custom context selection chips in card grid
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val contextOptionList = listOf("Fasting", "Before Meal", "After Meal", "Bedtime", "Other")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                contextOptionList.take(3).forEach { contextVal ->
                                    val active = context == contextVal
                                    Button(
                                        modifier = Modifier.weight(1f),
                                        onClick = { context = contextVal },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (active) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text(contextVal, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                contextOptionList.drop(3).forEach { contextVal ->
                                    val active = context == contextVal
                                    Button(
                                        modifier = Modifier.weight(1f),
                                        onClick = { context = contextVal },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (active) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text(contextVal, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                                // placeholder balance
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 3. DATETIME AND NOTES SECTION CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Date & Timeline Info",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = time,
                                onValueChange = { time = it },
                                label = { Text("Time (HH:MM)") },
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (e.g., Checked with fingerstick)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 2
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.glucValue = value.trim()
                            viewModel.glucMealContext = context.trim()
                            viewModel.glucDate = date.trim()
                            viewModel.glucTime = time.trim()
                            viewModel.glucNotes = notes.trim()
                            onSave()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(42.dp),
                        enabled = !isError
                    ) {
                        Text("Save Details")
                    }
                }
            }
        }
    }
}

