package com.example.ui.screens.settings

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
fun SettingsScreen(
    viewModel: GlucoViewModel,
    onBackClick: () -> Unit
) {
    val currentProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()



    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gluco_auth_prefs", android.content.Context.MODE_PRIVATE) }
    val linkedEmail = remember(loggedInUser) { prefs.getString("user_email_$loggedInUser", "") ?: "" }

    // Archive, backup, and restore states
    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importErrorMsg by remember { mutableStateOf<String?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Document activity launchers for file-based saving and loading
    val fileExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(exportJsonText.toByteArray(Charsets.UTF_8))
                }
                android.widget.Toast.makeText(context, "Backup successfully saved to file!", android.widget.Toast.LENGTH_LONG).show()
                showExportDialog = false
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    viewModel.importAppDataFromJSON(content) { success ->
                        if (success) {
                            android.widget.Toast.makeText(context, "App backup restored successfully!", android.widget.Toast.LENGTH_LONG).show()
                            showImportDialog = false
                        } else {
                            importErrorMsg = "Failed to parse backup JSON. Invalid file format."
                            showImportDialog = true
                        }
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Read error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val googleAccountPickerLauncherForDrive = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrEmpty()) {
                val account = android.accounts.Account(accountName, "com.google")
                val accountManager = android.accounts.AccountManager.get(context)
                val activity = context as? android.app.Activity
                if (activity != null) {
                    accountManager.getAuthToken(
                        account,
                        "oauth2:https://www.googleapis.com/auth/drive.file",
                        null,
                        activity,
                        { future ->
                            try {
                                val bundle = future.result
                                val token = bundle.getString(android.accounts.AccountManager.KEY_AUTHTOKEN)
                                if (!token.isNullOrEmpty()) {
                                    viewModel.setGoogleDriveAccessToken(token)
                                    android.widget.Toast.makeText(context, "Successfully authorized Google Drive automatically for $accountName!", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to retrieve authorization token.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("GoogleDriveAuth", "Failed to retrieve token", e)
                                android.widget.Toast.makeText(context, "Authorization failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        null
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Connection/Auth status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                        onClick = {
                            viewModel.logout()
                            onBackClick()
                        },
                        modifier = Modifier.testTag("logout_button_settings_screen"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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

            // Theme Customization Card
            var showThemeDropdown by remember { mutableStateOf(false) }
            val selectedThemeCode by viewModel.selectedTheme.collectAsStateWithLifecycle()
            val lightThemes = listOf(
                Triple("arctic", "Arctic Light", "Light / Clean SaaS theme"),
                Triple("sakura", "Sakura Blossom", "Light / Romantic Pastel Pink"),
                Triple("lemon_zest", "Lemon Zest", "Light / Warm Sunny Yellow"),
                Triple("desert_sand", "Desert Sand", "Light / Terracotta Sand Style"),
                Triple("slate_pro", "Slate Pro", "Light / Professional Cool Gray"),
                Triple("coral_bloom", "Coral Bloom", "Light / Fresh Energetic Coral")
            )
            val darkThemes = listOf(
                Triple("ocean_depth", "Ocean Depth", "Dark / High-Contrast Marine theme"),
                Triple("aurora", "Aurora Indigo", "Dark / Purple Neon style"),
                Triple("midnight_carbon", "Midnight Carbon", "Dark / Minimalist Slate theme"),
                Triple("forest_calm", "Forest Calm", "Dark / Organic Deep Green"),
                Triple("neon_noir", "Neon Noir", "Dark / Vibrant Cyberpunk accents"),
                Triple("royal_ink", "Royal Ink", "Dark / Premium Indigo Sapphire")
            )
            val themeOptions = lightThemes + darkThemes
            val activeThemePair = themeOptions.find { it.first == selectedThemeCode } ?: themeOptions[0]

            Card(
                modifier = Modifier.fillMaxWidth().testTag("theme_customization_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "App Color Theme",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose from 12 eye-safe mastercraft themes tailored for clinical environments, day logging, and night tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = activeThemePair.second,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active Interface Theme") },
                            modifier = Modifier.fillMaxWidth().testTag("theme_selector_dropdown_input"),
                            shape = RoundedCornerShape(12.dp),
                            supportingText = { Text(activeThemePair.third) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Palette logo Theme",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showThemeDropdown = !showThemeDropdown }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Toggle Theme Dropdown")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = showThemeDropdown,
                            onDismissRequest = { showThemeDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            // LIGHT THEMES HEADER
                            DropdownMenuItem(
                                enabled = false,
                                onClick = {},
                                text = {
                                    Text(
                                        text = "LIGHT THEMES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            )

                            lightThemes.forEach { theme ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        val indicatorColor = when (theme.first) {
                                            "sakura" -> Color(0xFFE91E8C)
                                            "arctic" -> Color(0xFF0066CC)
                                            "lemon_zest" -> Color(0xFFD4A017)
                                            "desert_sand" -> Color(0xFFC2853A)
                                            "slate_pro" -> Color(0xFF3D5A80)
                                            "coral_bloom" -> Color(0xFFFF5733)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(indicatorColor, RoundedCornerShape(4.dp))
                                        )
                                    },
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(theme.second, fontWeight = if (theme.first == selectedThemeCode) FontWeight.Bold else FontWeight.Normal)
                                            Text(
                                                text = "Light",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectTheme(theme.first)
                                        showThemeDropdown = false
                                    }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // DARK THEMES HEADER
                            DropdownMenuItem(
                                enabled = false,
                                onClick = {},
                                text = {
                                    Text(
                                        text = "DARK THEMES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            )

                            darkThemes.forEach { theme ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        val indicatorColor = when (theme.first) {
                                            "ocean_depth" -> Color(0xFF3B9EFF)
                                            "aurora" -> Color(0xFFA78BFA)
                                            "midnight_carbon" -> Color(0xFFE8E8E8)
                                            "forest_calm" -> Color(0xFF5CB85C)
                                            "neon_noir" -> Color(0xFFFF2D78)
                                            "royal_ink" -> Color(0xFF6C63FF)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(indicatorColor, RoundedCornerShape(4.dp))
                                        )
                                    },
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(theme.second, fontWeight = if (theme.first == selectedThemeCode) FontWeight.Bold else FontWeight.Normal)
                                            Text(
                                                text = "Dark",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectTheme(theme.first)
                                        showThemeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // App Data Management (Backup, Restore & Reset)
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
                        text = "App Data Backup & Reset",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Securely backup your healthcare logs, restore from previous active archives, or wipe your local records to start fresh.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Export Button
                        OutlinedButton(
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                viewModel.exportAppDataToJSON { json ->
                                    exportJsonText = json ?: "[]"
                                    showExportDialog = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export App Data",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Data", fontSize = 12.sp)
                        }

                        // Import Button
                        OutlinedButton(
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                importJsonText = ""
                                importErrorMsg = null
                                showImportDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Import App Data",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Data", fontSize = 12.sp)
                        }
                    }

                    // Reset Data Button
                    Button(
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        onClick = { showClearConfirmDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All App Data",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All App Data", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Google Drive Cloud Backup Card
            val gdSyncEnabled by viewModel.googleDriveSyncEnabled.collectAsStateWithLifecycle()
            val gdAccessToken by viewModel.googleDriveAccessToken.collectAsStateWithLifecycle()
            val gdLastSyncTime by viewModel.googleDriveLastSyncTime.collectAsStateWithLifecycle()
            val gdSyncing by viewModel.isGoogleDriveSyncing.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier.fillMaxWidth().testTag("google_drive_sync_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Google Drive Sync",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Google Drive Cloud Backup",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "Synchronize your clinical database directly with your personal Google Drive storage automatically in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ${if (gdAccessToken.isNotEmpty()) "Connected (Auto-backup Active)" else "Disconnected"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (gdAccessToken.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )

                        if (gdAccessToken.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.disableGoogleDriveSync() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }

                    Text(
                        text = "Last Synced: $gdLastSyncTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Backup now button
                        Button(
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !gdSyncing && gdAccessToken.isNotEmpty(),
                            onClick = {
                                viewModel.backupToGoogleDrive { success, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            if (gdSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Backup to Drive", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Now", fontSize = 12.sp)
                            }
                        }

                        // Restore now button
                        OutlinedButton(
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !gdSyncing && gdAccessToken.isNotEmpty(),
                            onClick = {
                                viewModel.restoreFromGoogleDrive { success, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            if (gdSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Restore from Drive", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Now", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // App Updates Card
            val updateStatus by viewModel.updateCheckStatus.collectAsStateWithLifecycle()
            val latestVersion by viewModel.latestReleaseVersion.collectAsStateWithLifecycle()
            val latestApkUrl by viewModel.latestReleaseApkUrl.collectAsStateWithLifecycle()
            val releaseNotes by viewModel.latestReleaseNotes.collectAsStateWithLifecycle()
            val dlProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
            val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
            val dlStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier.fillMaxWidth().testTag("app_updates_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "App Version & Updates",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Version",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = { viewModel.checkForAppUpdates() },
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isDownloading
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Check update", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Updates", fontSize = 12.sp)
                        }
                    }

                    if (updateStatus != null) {
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Status: $updateStatus",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (updateStatus?.contains("Update") == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!latestApkUrl.isNullOrEmpty()) {
                                if (!releaseNotes.isNullOrEmpty()) {
                                    Text(
                                        text = "Release Notes:\n$releaseNotes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Download progress section
                                if (isDownloading || dlStatus != null) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isDownloading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                } else if (dlProgress >= 1f) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "Done",
                                                        tint = Color(0xFF4CAF50),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(
                                                    text = dlStatus ?: "",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (isDownloading) {
                                                LinearProgressIndicator(
                                                    progress = dlProgress,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                )
                                                Text(
                                                    text = "${(dlProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.align(Alignment.End)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Download & Install button
                                Button(
                                    onClick = {
                                        viewModel.downloadAndInstallApk(latestApkUrl!!)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    enabled = !isDownloading
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Downloading...")
                                    } else {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = "Install")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download & Install Update")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    // Backup & Restore Modals
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Export Icons", tint = MaterialTheme.colorScheme.primary)
                    Text("Export App Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Save your backup file directly to your storage. You can transfer this file to fully restore your logs and profiles on any device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = {
                            fileExportLauncher.launch("glucolog_backup_${System.currentTimeMillis()}.json")
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Download outline")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save as JSON File")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Import Icons", tint = MaterialTheme.colorScheme.primary)
                    Text("Import Archive Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Restore from an exported backup file (*.json). Warning: This will overwrite active database metrics!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = {
                            fileImportLauncher.launch(arrayOf("*/*"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Browse folder icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup JSON File")
                    }
                    if (importErrorMsg != null) {
                        Text(
                            text = importErrorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Wipe Icons", tint = MaterialTheme.colorScheme.error)
                    Text("Confirm Hard Reset", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to permanently delete all health logs, medication records, profiles, and reminders from this device? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllAppData {
                            android.widget.Toast.makeText(context, "All app data cleared successfully!", android.widget.Toast.LENGTH_LONG).show()
                            showClearConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Permanently Wipe Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Keep My Data")
                }
            }
        )
    }
}

