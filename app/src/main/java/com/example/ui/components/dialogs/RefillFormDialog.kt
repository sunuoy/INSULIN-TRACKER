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
fun RefillFormDialog(
    viewModel: GlucoViewModel,
    editingLog: CartridgeRefillLog? = null,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var capacity by remember { mutableStateOf(editingLog?.capacity?.toInt()?.toString() ?: "300") }
    var date by remember { mutableStateOf(if (editingLog != null) viewModel.formatEpochToDateOnly(editingLog.dateTimeMillis) else viewModel.formatEpochToDateOnly(viewModel.getCurrentTimeMillis())) }
    var time by remember { mutableStateOf(if (editingLog != null) viewModel.formatEpochToTimeOnly(editingLog.dateTimeMillis) else viewModel.formatEpochToTimeOnly(viewModel.getCurrentTimeMillis())) }
    var actionType by remember { mutableStateOf(editingLog?.actionType ?: "Refill") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("refill_form_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (editingLog != null) "Edit Cartridge Log" else "Refill / Change Cartridge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Specify size capacity, date and time of the insulin cartridge change/refill.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                // Quick choices
                if (editingLog == null) {
                    Text("Preset Sizes", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("100", "150", "300").forEach { preset ->
                            val isSelected = capacity == preset
                            Button(
                                onClick = { capacity = preset },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("${preset}U", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Capacity Input
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it.filter { char -> char.isDigit() } },
                    label = { Text("Capacity Size (Units)") },
                    modifier = Modifier.fillMaxWidth().testTag("refill_capacity_input"),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = { Text("Units", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Date & Time inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1.2f).testTag("refill_date_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (HH:MM)") },
                        modifier = Modifier.weight(0.8f).testTag("refill_time_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Action Type Selector
                var showActionDropdown by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = actionType,
                        onValueChange = { actionType = it },
                        label = { Text("Log Event Title / Action") },
                        modifier = Modifier.fillMaxWidth().testTag("refill_action_type_input"),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            IconButton(onClick = { showActionDropdown = !showActionDropdown }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showActionDropdown,
                        onDismissRequest = { showActionDropdown = false }
                    ) {
                        listOf("Refill", "Change (Empty)", "Size Change", "Pen Inserted").forEach { title ->
                            DropdownMenuItem(
                                text = { Text(title) },
                                onClick = {
                                    actionType = title
                                    showActionDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val capacityVal = capacity.toDoubleOrNull() ?: 300.0
                            if (editingLog != null) {
                                val calendar = viewModel.composeCalendarFromDateStrAndTimeStr(date, time)
                                val updatedLog = editingLog.copy(
                                    capacity = capacityVal,
                                    dateTimeMillis = calendar.timeInMillis,
                                    actionType = actionType
                                )
                                viewModel.saveRefillLog(updatedLog)
                            } else {
                                viewModel.refillCartridge(capacityVal, date, time)
                            }
                            onSave()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (editingLog != null) "Update Log" else "Update Cartridge")
                    }
                }
            }
        }
    }
}

