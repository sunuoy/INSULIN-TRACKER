package com.example.ui.screens

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

data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val screen: AppScreen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoAppLayout(viewModel: GlucoViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isPasscodeEnabled by viewModel.isPasscodeEnabled.collectAsStateWithLifecycle()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsStateWithLifecycle()
    val latestVersion by viewModel.latestReleaseVersion.collectAsStateWithLifecycle()
    val changeCategory by viewModel.updateChangeCategory.collectAsStateWithLifecycle()
    val latestApkUrl by viewModel.latestReleaseApkUrl.collectAsStateWithLifecycle()
    val releaseNotes by viewModel.latestReleaseNotes.collectAsStateWithLifecycle()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel)
    } else if (isPasscodeEnabled && isAppLocked) {
        PasscodeLockScreen(viewModel = viewModel)
    } else {
        val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
        val rawInsulinList by viewModel.insulinRecords.collectAsStateWithLifecycle()
    val rawGlucoseList by viewModel.glucoseReadings.collectAsStateWithLifecycle()
    val profilesState by viewModel.userProfile.collectAsStateWithLifecycle()

    var showInsulinDialog by remember { mutableStateOf(false) }
    var showGlucoseDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showRefillFormDialog by remember { mutableStateOf(false) }
    var editingRefillLog by remember { mutableStateOf<CartridgeRefillLog?>(null) }
    var showBloodPressureDialog by remember { mutableStateOf(false) }
    var showStepDialog by remember { mutableStateOf(false) }
    var showHypoAlert by remember { mutableStateOf(false) }
    var hypoAlertValue by remember { mutableStateOf(0.0) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                drawerTonalElevation = 6.dp
            ) {
                val gdAccessTokenForDrawer by viewModel.googleDriveAccessToken.collectAsStateWithLifecycle()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- PATIENT PROFILE HEADER CARD ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                // Avatar Circle with initials
                                val patientName = profilesState.userName.ifEmpty { "Patient" }
                                val initials = patientName.trim().split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .joinToString("")
                                    .ifEmpty { "P" }

                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 4.dp
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { scope.launch { drawerState.close() } },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Close Drawer",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = profilesState.userName.ifEmpty { "Clinical Patient" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (profilesState.doctorName.isNotEmpty()) {
                                Text(
                                    text = "Dr. ${profilesState.doctorName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Glucose target range pill badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Target: ${profilesState.targetGlucoseMin.toInt()}-${profilesState.targetGlucoseMax.toInt()} ${profilesState.glucoseUnit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // --- SECTION 1: HEALTH & CLINICAL TRACKERS ---
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "HEALTH & CLINICAL TRACKERS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 0.8.sp
                        )

                        listOf(
                            Triple("Dashboard", Icons.Default.Home, AppScreen.HOME),
                            Triple("Clinical Logs", Icons.Default.List, AppScreen.HISTORY),
                            Triple("Steps Tracker", Icons.Default.DirectionsWalk, AppScreen.STEPS),
                            Triple("Reminders", Icons.Default.Notifications, AppScreen.REMINDERS),
                            Triple("Reports & Insights", Icons.Default.Assessment, AppScreen.REPORTS),
                            Triple("Clinical Profile", Icons.Default.Person, AppScreen.PROFILE)
                        ).forEach { (title, icon, screen) ->
                            val isSelected = currentScreen == screen
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    viewModel.navigateTo(screen)
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedContainerColor = Color.Transparent,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // --- SECTION 2: DATA MANAGEMENT & CLOUD SYNC ---
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "DATA & CLOUD SYNC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 0.8.sp
                        )

                        // Full Data Sync Action Item
                        Surface(
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.triggerUploadSync()
                                android.widget.Toast.makeText(context, "Full medical database sync initiated!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Sync All Data",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Database",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Google Drive Sync Action Item
                        Surface(
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (gdAccessTokenForDrawer.isEmpty()) {
                                    android.widget.Toast.makeText(context, "Google Drive not configured. Please go to Settings and enter your Access Token.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Initiating Google Drive backup...", android.widget.Toast.LENGTH_SHORT).show()
                                    viewModel.backupToGoogleDrive { success, msg ->
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Google Drive Sync",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (gdAccessTokenForDrawer.isNotEmpty()) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = if (gdAccessTokenForDrawer.isNotEmpty()) "Connected" else "Cloud",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (gdAccessTokenForDrawer.isNotEmpty()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // --- SECTION 3: SYSTEM & PREFERENCES ---
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "SYSTEM & PREFERENCES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 0.8.sp
                        )

                        val isSettingsSelected = currentScreen == AppScreen.SETTINGS
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = "Settings & Configuration",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            selected = isSettingsSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.navigateTo(AppScreen.SETTINGS)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = if (isSettingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.height(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // --- FOOTER: LOGOUT & EXIT ---
                    Surface(
                        onClick = {
                            viewModel.logout()
                            android.widget.Toast.makeText(context, "Logged out successfully.", android.widget.Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Exit App",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Exit App / Logout",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    ) {
        val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val isSettings = currentScreen == AppScreen.SETTINGS
                TopAppBar(
                    title = {
                        val titleText = when (currentScreen) {
                            AppScreen.HOME -> "Clinical Dashboard"
                            AppScreen.HISTORY -> "Clinical Logs History"
                            AppScreen.STEPS -> "Steps Tracker"
                            AppScreen.REMINDERS -> "Medication Reminders"
                            AppScreen.REPORTS -> "Interactive Reports"
                            AppScreen.PROFILE -> "User Settings & Profile"
                            AppScreen.SETTINGS -> "Settings"
                            else -> "System Settings"
                        }
                        Text(
                            text = titleText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        if (isSettings) {
                            IconButton(onClick = { viewModel.navigateTo(AppScreen.PROFILE) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier
                                    .testTag("menu_nav_icon_button")
                                    .padding(horizontal = 4.dp)
                                    .size(width = 84.dp, height = 48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Menu",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (!isSettings) {
                            IconButton(
                                onClick = {
                                    viewModel.triggerUploadSync()
                                    android.widget.Toast.makeText(context, "Syncing medical data...", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("action_sync_data")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync medical data",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_navigation_bar"),
                    tonalElevation = 6.dp
                ) {
                    listOf(
                        NavigationItem("Home", Icons.Default.Home, Icons.Outlined.Home, AppScreen.HOME),
                        NavigationItem("Logs", Icons.Default.List, Icons.Outlined.List, AppScreen.HISTORY),
                        NavigationItem("Steps", Icons.Default.DirectionsWalk, Icons.Outlined.DirectionsWalk, AppScreen.STEPS),
                        NavigationItem("Reminders", Icons.Default.Notifications, Icons.Outlined.Notifications, AppScreen.REMINDERS),
                        NavigationItem("Reports", Icons.Default.Assessment, Icons.Outlined.Assessment, AppScreen.REPORTS)
                    ).forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(item.screen) },
                            label = { Text(item.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onLogInsulinClick = {
                            viewModel.resetInsulinForm()
                            showInsulinDialog = true
                        },
                        onLogGlucoseClick = {
                            viewModel.resetGlucoseForm()
                            showGlucoseDialog = true
                        },
                        onRefillClick = {
                            editingRefillLog = null
                            showRefillFormDialog = true
                        },
                        onLogBloodPressureClick = {
                            viewModel.resetBloodPressureForm()
                            showBloodPressureDialog = true
                        },
                        onLogStepsClick = {
                            viewModel.resetStepForm()
                            showStepDialog = true
                        }
                    )
                    AppScreen.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        onEditInsulin = { record ->
                            viewModel.prepareEditInsulin(record)
                            showInsulinDialog = true
                        },
                        onEditGlucose = { reading ->
                            viewModel.prepareEditGlucose(reading)
                            showGlucoseDialog = true
                        },
                        onEditRefill = { log ->
                            editingRefillLog = log
                            showRefillFormDialog = true
                        },
                        onEditBloodPressure = { record ->
                            viewModel.prepareEditBloodPressure(record)
                            showBloodPressureDialog = true
                        },
                        onAddInsulinClick = {
                            viewModel.resetInsulinForm()
                            showInsulinDialog = true
                        },
                        onAddGlucoseClick = {
                            viewModel.resetGlucoseForm()
                            showGlucoseDialog = true
                        },
                        onAddRefillClick = {
                            editingRefillLog = null
                            showRefillFormDialog = true
                        },
                        onAddBloodPressureClick = {
                            viewModel.resetBloodPressureForm()
                            showBloodPressureDialog = true
                        }
                    )
                    AppScreen.REMINDERS -> RemindersScreen(
                        viewModel = viewModel,
                        onAddReminderClick = {
                            viewModel.resetReminderForm()
                            showReminderDialog = true
                        },
                        onEditReminder = { reminder ->
                            viewModel.prepareEditReminder(reminder)
                            showReminderDialog = true
                        }
                    )
                    AppScreen.STEPS -> StepsScreen(
                        viewModel = viewModel,
                        onAddStepClick = {
                            viewModel.resetStepForm()
                            showStepDialog = true
                        },
                        onEditStep = { record ->
                            viewModel.prepareEditStep(record)
                            showStepDialog = true
                        }
                    )
                    AppScreen.REPORTS -> ReportsScreen(viewModel = viewModel)
                    AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { viewModel.navigateTo(AppScreen.PROFILE) }
                    )
                }
            }
        }
    }

    if (isLoggedIn && isUpdateAvailable && latestVersion != null) {
        val isDownloading by viewModel.isDownloading.collectAsState()
        val dlProgress by viewModel.downloadProgress.collectAsState()
        val dlStatus by viewModel.downloadStatus.collectAsState()

        AlertDialog(
            onDismissRequest = { 
                if (isDownloading) {
                    viewModel.cancelApkDownload()
                }
                viewModel.dismissUpdateDialog() 
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Update Available",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDownloading) "Downloading Update..." else "New Update Available!",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDownloading) {
                        Text(
                            text = dlStatus ?: "Downloading...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = dlProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${(dlProgress * 100).toInt()}% completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.align(Alignment.End)
                        )
                    } else {
                        Text(
                            text = "A newer version of GlucoLog Tracker is available.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Latest Release: $latestVersion",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = changeCategory,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (!releaseNotes.isNullOrEmpty()) {
                            Text(
                                text = "What's New:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = releaseNotes!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(
                        onClick = {
                            latestApkUrl?.let {
                                viewModel.downloadAndInstallApk(it)
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Update Now")
                    }
                }
            },
            dismissButton = {
                if (isDownloading) {
                    TextButton(
                        onClick = {
                            viewModel.cancelApkDownload()
                            viewModel.dismissUpdateDialog()
                        }
                    ) {
                        Text("Cancel")
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.dismissUpdateDialog() }
                    ) {
                        Text("Later")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Forms
    if (showInsulinDialog) {
        InsulinFormDialog(
            viewModel = viewModel,
            onDismiss = { showInsulinDialog = false },
            onSave = {
                viewModel.saveInsulinRecord()
                showInsulinDialog = false
            }
        )
    }

    if (showGlucoseDialog) {
        GlucoseFormDialog(
            viewModel = viewModel,
            onDismiss = { showGlucoseDialog = false },
            onSave = {
                val valueDouble = viewModel.glucValue.toDoubleOrNull() ?: 100.0
                val unit = profilesState.glucoseUnit
                val threshold = if (unit == "mmol/L") 3.05 else 55.0

                viewModel.saveGlucoseReading()
                showGlucoseDialog = false

                if (valueDouble < threshold) {
                    hypoAlertValue = valueDouble
                    showHypoAlert = true
                    viewModel.playEmergencySound()
                }
            }
        )
    }

    if (showReminderDialog) {
         ReminderFormDialog(
             viewModel = viewModel,
             onDismiss = { showReminderDialog = false },
             onSave = {
                 viewModel.saveReminder()
                 showReminderDialog = false
             }
         )
     }

     if (showRefillFormDialog) {
         RefillFormDialog(
             viewModel = viewModel,
             editingLog = editingRefillLog,
             onDismiss = { showRefillFormDialog = false },
             onSave = { showRefillFormDialog = false }
         )
     }

     if (showBloodPressureDialog) {
         BloodPressureFormDialog(
             viewModel = viewModel,
             onDismiss = { showBloodPressureDialog = false },
             onSave = {
                 viewModel.saveBloodPressureRecord()
                 showBloodPressureDialog = false
             }
         )
     }

      if (showStepDialog) {
          StepFormDialog(
              viewModel = viewModel,
              onDismiss = { showStepDialog = false },
              onSave = {
                  viewModel.saveStepRecord()
                  showStepDialog = false
              }
          )
      }

      if (showHypoAlert) {
          AlertDialog(
              onDismissRequest = { showHypoAlert = false },
              title = {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                      Icon(
                          imageVector = Icons.Default.Warning,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.error,
                          modifier = Modifier.size(28.dp)
                      )
                      Text(
                          text = "Critical Hypoglycemia Warning!",
                          color = MaterialTheme.colorScheme.error,
                          fontWeight = FontWeight.Bold
                      )
                  }
              },
              text = {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text(
                          text = "Your blood sugar is dangerously low: %.1f %s.".format(Locale.US, hypoAlertValue, profilesState.glucoseUnit),
                          style = MaterialTheme.typography.bodyLarge,
                          fontWeight = FontWeight.SemiBold
                      )
                      Text(
                          text = "Please consume fast-acting carbohydrates immediately (e.g., fruit juice, candy, or glucose tablets) and notify your contact if needed.",
                          style = MaterialTheme.typography.bodyMedium
                      )
                      if (profilesState.emergencyContactPhone.isNotEmpty()) {
                          Text(
                              text = "Emergency Contact: ${profilesState.emergencyContactName} (${profilesState.emergencyContactPhone})",
                              style = MaterialTheme.typography.bodyMedium,
                              fontWeight = FontWeight.Bold,
                              color = MaterialTheme.colorScheme.primary
                          )
                      } else {
                          Text(
                              text = "Note: No Emergency Contact configured in Settings.",
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.outline
                          )
                      }
                  }
              },
              confirmButton = {
                  if (profilesState.emergencyContactPhone.isNotEmpty()) {
                      Button(
                          onClick = {
                              viewModel.launchEmergencySMSIntent(context, hypoAlertValue)
                              showHypoAlert = false
                          },
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                      ) {
                          Text("Send Emergency SMS")
                      }
                  }
              },
              dismissButton = {
                  TextButton(onClick = { showHypoAlert = false }) {
                      Text("I'm Okay / Dismiss")
                  }
              }
          )
      }

    }
}
