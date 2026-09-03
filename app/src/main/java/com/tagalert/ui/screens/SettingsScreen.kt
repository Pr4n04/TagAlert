package com.tagalert.ui.screens

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tagalert.data.local.UserPreferences
import com.tagalert.service.FindHubNotificationListener
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val countdownSeconds by viewModel.countdownSeconds.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val safeZonesEnabled by viewModel.safeZonesEnabled.collectAsState()

    var showCountdownDialog by remember { mutableStateOf(false) }
    var editingDeviceName by remember { mutableStateOf(false) }
    var nameInput by remember(deviceName) { mutableStateOf(deviceName) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var registerName by remember { mutableStateOf(deviceName) }
    var registerId by remember { mutableStateOf("") }

    // Check notification listener status
    val isNotificationListenerEnabled = remember {
        val cn = ComponentName(context, FindHubNotificationListener::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        flat != null && flat.contains(cn.flattenToString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- Find Hub Integration Section ---
            Text(
                "Find Hub Integration",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    SettingItem(
                        icon = Icons.Outlined.OpenInNew,
                        title = "Open Find Hub",
                        subtitle = "Pair your tracker in the Find Hub app",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.adm")
                                setPackage("com.android.vending")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to web
                                val webIntent = Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.adm"))
                                context.startActivity(webIntent)
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "Notification Listener",
                        subtitle = if (isNotificationListenerEnabled) "Enabled" else "Required for left-behind alerts",
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(
                        icon = Icons.Outlined.Edit,
                        title = "Register Device Manually",
                        subtitle = "Enter tracker name and ID after pairing in Find Hub",
                        onClick = { showRegisterDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Device Section ---
            Text(
                "Device",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Device Name
                    SettingItem(
                        icon = Icons.Outlined.Label,
                        title = "Device Name",
                        subtitle = deviceName,
                        onClick = { editingDeviceName = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Countdown
                    SettingItem(
                        icon = Icons.Outlined.Timer,
                        title = "Left Behind Countdown",
                        subtitle = "${countdownSeconds} seconds",
                        onClick = { showCountdownDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Alerts Section ---
            Text(
                "Alerts",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Vibration
                    SettingToggle(
                        icon = Icons.Outlined.Vibration,
                        title = "Vibration",
                        subtitle = "Vibrate when left behind",
                        checked = vibrationEnabled,
                        onCheckedChange = viewModel::setVibrationEnabled
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Sound
                    SettingToggle(
                        icon = Icons.Outlined.VolumeUp,
                        title = "Sound",
                        subtitle = "Play alert sound",
                        checked = soundEnabled,
                        onCheckedChange = viewModel::setSoundEnabled
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // Safe Zones
                    SettingToggle(
                        icon = Icons.Outlined.Home,
                        title = "Safe Zones",
                        subtitle = "Suppress alerts at known locations",
                        checked = safeZonesEnabled,
                        onCheckedChange = viewModel::setSafeZonesEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- System Section ---
            Text(
                "System",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    SettingItem(
                        icon = Icons.Outlined.Bluetooth,
                        title = "Bluetooth Permissions",
                        subtitle = "Manage BLE access",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(
                        icon = Icons.Outlined.LocationOn,
                        title = "Location Permissions",
                        subtitle = "Background location access",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Notification Settings",
                        subtitle = "Manage notification channels",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Version
            Text(
                "TagAlert v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Countdown Dialog
    if (showCountdownDialog) {
        CountdownDialog(
            currentSeconds = countdownSeconds,
            onDismiss = { showCountdownDialog = false },
            onConfirm = { seconds ->
                viewModel.setCountdownSeconds(seconds)
                showCountdownDialog = false
            }
        )
    }

    // Device Name Dialog
    if (editingDeviceName) {
        AlertDialog(
            onDismissRequest = { editingDeviceName = false },
            title = { Text("Device Name") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDeviceName(nameInput)
                    editingDeviceName = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDeviceName = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual Device Registration Dialog
    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Register Find Hub Tracker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "After pairing your tracker in the Find Hub app, enter its details here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = registerName,
                        onValueChange = { registerName = it },
                        label = { Text("Tracker Name") },
                        placeholder = { Text("e.g. My Keys") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = registerId,
                        onValueChange = { registerId = it },
                        label = { Text("Device ID") },
                        placeholder = { Text("e.g. CM916-ABCD or MAC address") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (registerName.isNotBlank() && registerId.isNotBlank()) {
                            viewModel.registerManualDevice(registerName, registerId)
                            showRegisterDialog = false
                        }
                    },
                    enabled = registerName.isNotBlank() && registerId.isNotBlank()
                ) {
                    Text("Register")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun CountdownDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentSeconds.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Left Behind Countdown") },
        text = {
            Column {
                Text(
                    "How long to wait before alerting you?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "${sliderValue.toInt()} seconds",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 15f..300f,
                    steps = 18
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("15s", style = MaterialTheme.typography.labelSmall)
                    Text("5 min", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    when {
                        sliderValue.toInt() <= 30 -> "Short — catches quick exits"
                        sliderValue.toInt() <= 120 -> "Balanced — good for daily use"
                        else -> "Long — only for deliberate check-ins"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValue.toInt()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}