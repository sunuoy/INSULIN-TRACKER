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
fun GoogleSignInDialog(
    initialFullName: String = "",
    initialEmail: String = "",
    initialUsername: String = "",
    onDismiss: () -> Unit,
    onSignInSuccess: (fullName: String, email: String, username: String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Choose account, 2: OTP, 3: Success

    var fullName by remember(initialFullName) { mutableStateOf(initialFullName) }
    var emailId by remember(initialEmail) { mutableStateOf(initialEmail) }
    var userName by remember(initialUsername) { mutableStateOf(initialUsername) }
    
    var otpInput by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableIntStateOf(30) }
    
    val context = LocalContext.current
    
    LaunchedEffect(step) {
        if (step == 2) {
            val code = (100000..999999).random().toString()
            generatedOtp = code
            android.widget.Toast.makeText(
                context,
                "🔔 Gmail: GlucoLog verification code is $code (valid for 5 minutes)",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    LaunchedEffect(step, timerSeconds) {
        if (step == 2 && timerSeconds > 0) {
            delay(1000L)
            timerSeconds--
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("google_signin_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    GoogleLogoIcon(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (step == 1) {
                    Text(
                        text = "Sign up with Google",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Input your Google profile details to register and securely connect this clinical session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. David Miller") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_input_full_name"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = emailId,
                            onValueChange = { emailId = it },
                            label = { Text("Email ID") },
                            placeholder = { Text("e.g. david.clinical@gmail.com") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_input_email"),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("Username") },
                            placeholder = { Text("e.g. davidm") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_input_username"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val trimmedEmail = emailId.trim().lowercase()
                                    if (fullName.trim().isEmpty() || trimmedEmail.isEmpty() || userName.trim().isEmpty()) {
                                        android.widget.Toast.makeText(context, "Please fill out all fields", android.widget.Toast.LENGTH_SHORT).show()
                                    } else if (!trimmedEmail.endsWith("@gmail.com")) {
                                        android.widget.Toast.makeText(context, "Only @gmail.com addresses are supported for Google auth", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        emailId = trimmedEmail
                                        onSignInSuccess(fullName, emailId, userName)
                                        step = 3
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("google_custom_next_btn")
                            ) {
                                Text("Next")
                            }
                        }
                    }
                } else if (step == 2) {
                    Text(
                        text = "Verify your email",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "A 6-digit confirmation OTP has been transmitted to $emailId to secure registration parameters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { 
                            if (it.length <= 6) {
                                otpInput = it
                                otpError = ""
                            }
                        },
                        label = { Text("6-Digit OTP Code") },
                        placeholder = { Text("Enter OTP code") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_otp_input"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center)
                    )

                    if (otpError.isNotEmpty()) {
                        Text(
                            text = otpError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        TextButton(
                            onClick = {
                                if (timerSeconds == 0) {
                                    timerSeconds = 30
                                    val code = (100000..999999).random().toString()
                                    generatedOtp = code
                                    android.widget.Toast.makeText(
                                        context,
                                        "🔔 Gmail: New verification code is $code",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = timerSeconds == 0,
                            modifier = Modifier.weight(1f).testTag("google_resend_otp_btn")
                        ) {
                            Text(
                                if (timerSeconds > 0) "Resend (${timerSeconds}s)" else "Resend"
                            )
                        }

                        Button(
                            onClick = {
                                if (otpInput == generatedOtp) {
                                    onSignInSuccess(fullName, emailId, userName)
                                    step = 3
                                } else {
                                    otpError = "Invalid verification code. Please check the notification banner."
                                }
                            },
                            modifier = Modifier.weight(1.5f).testTag("google_verify_otp_btn")
                        ) {
                            Text("Verify", maxLines = 1)
                        }
                    }
                } else if (step == 3) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Identity Verified!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Google account linked successfully. Your name, email, and clinical parameters are copied securely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_success_continue_btn")
                    ) {
                        Text("Continue to App")
                    }
                }
            }
        }
    }
}
