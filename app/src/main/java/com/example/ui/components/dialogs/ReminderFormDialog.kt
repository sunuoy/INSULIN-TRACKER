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
fun ReminderFormDialog(
    viewModel: GlucoViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var type by remember { mutableStateOf(viewModel.remType.ifEmpty { "Insulin" }) }
    var label by remember { mutableStateOf(viewModel.remLabel) }
    var hour by remember { mutableIntStateOf(viewModel.remHour) }
    var minute by remember { mutableIntStateOf(viewModel.remMinute) }
    var days by remember { mutableStateOf(viewModel.remDays.ifEmpty { "Daily" }) }
    var tone by remember { mutableStateOf(viewModel.remTone.ifEmpty { "Default" }) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("reminder_dialog"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (viewModel.selectedReminderIdToEdit != null) "Edit Reminder Alert" else "Add Reminder Alert",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text("Reminder Type", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val alertTypes = listOf("Insulin", "Blood Sugar Check", "Medication")
                    alertTypes.forEach { t ->
                        val active = type == t
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { type = t },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(t, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Reminder Label (e.g. Afternoon check)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_label_input")
                )

                // Inline Slider adjustment for hour and minute (robust and clickable!)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hour: $hour (0-23)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { if (hour > 0) hour-- }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) {
                                Text("-")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { if (hour < 23) hour++ }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) {
                                Text("+")
                            }
                        }
                    }
                    Slider(
                        value = hour.toFloat(),
                        onValueChange = { hour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Minute: $minute (0-59)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { if (minute > 0) minute -= 5 else if (minute == 0) minute = 55 }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) {
                                Text("-")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { if (minute < 55) minute += 5 else if (minute == 55) minute = 0 }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) {
                                Text("+")
                            }
                        }
                    }
                    Slider(
                        value = minute.toFloat(),
                        onValueChange = { minute = it.toInt() },
                        valueRange = 0f..59f,
                        steps = 58
                    )
                }

                Text("Reminder Tone", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val toneOptions = listOf("Default", "Gentle Chime", "Digital Alarm", "Medical Alert", "Zen Harp")
                    toneOptions.forEach { t ->
                        val active = tone == t
                        Button(
                            onClick = {
                                tone = t
                                playAdorableTone(t)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (active) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(t, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it },
                    label = { Text("Days Scheduled (e.g. Daily, Mon-Fri)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.remType = type
                            viewModel.remLabel = label
                            viewModel.remHour = hour
                            viewModel.remMinute = minute
                            viewModel.remDays = days
                            viewModel.remTone = tone
                            onSave()
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

