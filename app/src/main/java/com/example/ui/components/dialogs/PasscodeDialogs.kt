package com.example.ui.components.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.GlucoViewModel

@Composable
fun PasscodeSetupDialog(
    viewModel: GlucoViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) } // 1: Enter PIN, 2: Confirm PIN
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("passcode_setup_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (step == 1) "Set 4-Digit Passcode" else "Confirm Passcode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (step == 1)
                        "Create a 4-digit PIN to secure access to your clinical health records."
                    else
                        "Re-enter your 4-digit PIN to confirm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // PIN Input Indicator Bubbles
                val activeInput = if (step == 1) pin else confirmPin
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < activeInput.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Hidden/Numeric TextField for system keyboard input
                OutlinedTextField(
                    value = activeInput,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            errorMessage = null
                            if (step == 1) {
                                pin = newValue
                                if (newValue.length == 4) {
                                    step = 2
                                }
                            } else {
                                confirmPin = newValue
                                if (newValue.length == 4) {
                                    if (newValue == pin) {
                                        viewModel.setPasscode(newValue)
                                        Toast.makeText(context, "Passcode enabled successfully!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    } else {
                                        errorMessage = "Passcodes do not match. Please try again."
                                        confirmPin = ""
                                    }
                                }
                            }
                        }
                    },
                    label = { Text(if (step == 1) "Enter 4 Digits" else "Re-enter 4 Digits") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("passcode_setup_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (step == 2) {
                                step = 1
                                confirmPin = ""
                                errorMessage = null
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (step == 2) "Back" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (step == 1) {
                                if (pin.length == 4) {
                                    step = 2
                                    errorMessage = null
                                } else {
                                    errorMessage = "Please enter all 4 digits."
                                }
                            } else {
                                if (confirmPin == pin) {
                                    viewModel.setPasscode(confirmPin)
                                    Toast.makeText(context, "Passcode enabled successfully!", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                } else {
                                    errorMessage = "Passcodes do not match. Please try again."
                                    confirmPin = ""
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = if (step == 1) pin.length == 4 else confirmPin.length == 4
                    ) {
                        Text(if (step == 1) "Next" else "Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun PasscodeChangeDialog(
    viewModel: GlucoViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var stage by remember { mutableIntStateOf(1) } // 1: Verify Old, 2: Enter New, 3: Confirm New
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("passcode_change_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Key Icon",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = when (stage) {
                        1 -> "Enter Current Passcode"
                        2 -> "Enter New Passcode"
                        else -> "Confirm New Passcode"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when (stage) {
                        1 -> "Enter your current 4-digit PIN to authenticate."
                        2 -> "Create a new 4-digit PIN."
                        else -> "Re-enter your new 4-digit PIN."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                val activeInput = when (stage) {
                    1 -> currentPin
                    2 -> newPin
                    else -> confirmNewPin
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < activeInput.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                OutlinedTextField(
                    value = activeInput,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            errorMessage = null
                            when (stage) {
                                1 -> {
                                    currentPin = newValue
                                    if (newValue.length == 4) {
                                        if (viewModel.verifyPasscode(newValue)) {
                                            stage = 2
                                        } else {
                                            errorMessage = "Incorrect current passcode."
                                            currentPin = ""
                                        }
                                    }
                                }
                                2 -> {
                                    newPin = newValue
                                    if (newValue.length == 4) {
                                        stage = 3
                                    }
                                }
                                3 -> {
                                    confirmNewPin = newValue
                                    if (newValue.length == 4) {
                                        if (newValue == newPin) {
                                            viewModel.changePasscode(currentPin, newValue)
                                            Toast.makeText(context, "Passcode updated successfully!", Toast.LENGTH_SHORT).show()
                                            onSuccess()
                                        } else {
                                            errorMessage = "New passcodes do not match. Try again."
                                            confirmNewPin = ""
                                        }
                                    }
                                }
                            }
                        }
                    },
                    label = {
                        Text(
                            when (stage) {
                                1 -> "Current PIN"
                                2 -> "New 4-Digit PIN"
                                else -> "Confirm New PIN"
                            }
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            when (stage) {
                                1 -> {
                                    if (viewModel.verifyPasscode(currentPin)) {
                                        stage = 2
                                        errorMessage = null
                                    } else {
                                        errorMessage = "Incorrect current passcode."
                                        currentPin = ""
                                    }
                                }
                                2 -> {
                                    if (newPin.length == 4) {
                                        stage = 3
                                        errorMessage = null
                                    } else {
                                        errorMessage = "Please enter 4 digits."
                                    }
                                }
                                3 -> {
                                    if (confirmNewPin == newPin) {
                                        viewModel.changePasscode(currentPin, confirmNewPin)
                                        Toast.makeText(context, "Passcode updated successfully!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    } else {
                                        errorMessage = "New passcodes do not match."
                                        confirmNewPin = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = when (stage) {
                            1 -> currentPin.length == 4
                            2 -> newPin.length == 4
                            else -> confirmNewPin.length == 4
                        }
                    ) {
                        Text(if (stage == 3) "Update" else "Next")
                    }
                }
            }
        }
    }
}

@Composable
fun PasscodeDisableDialog(
    viewModel: GlucoViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("passcode_disable_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlock Icon",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Disable Passcode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Enter your current 4-digit PIN to turn off Passcode Protection on app open.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                            enteredPin = newValue
                            errorMessage = null
                            if (newValue.length == 4) {
                                if (viewModel.disablePasscode(newValue)) {
                                    Toast.makeText(context, "Passcode protection disabled", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                } else {
                                    errorMessage = "Incorrect passcode."
                                    enteredPin = ""
                                }
                            }
                        }
                    },
                    label = { Text("Current 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (viewModel.disablePasscode(enteredPin)) {
                                Toast.makeText(context, "Passcode protection disabled", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                errorMessage = "Incorrect passcode."
                                enteredPin = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = enteredPin.length == 4
                    ) {
                        Text("Turn Off")
                    }
                }
            }
        }
    }
}
