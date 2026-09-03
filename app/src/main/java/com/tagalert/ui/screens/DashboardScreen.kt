package com.tagalert.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tagalert.ui.theme.StatusGreen
import com.tagalert.ui.theme.StatusGray
import com.tagalert.ui.theme.StatusRed
import com.tagalert.ui.theme.StatusYellow
import com.tagalert.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    onToggleTracking: (Boolean) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val isTracking by viewModel.isTracking.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val lastSeenText by viewModel.lastSeenText.collectAsState()
    val deviceState by viewModel.deviceState.collectAsState()
    val lastSeenLocation by viewModel.lastSeenLocation.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TagAlert",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                    label = { Text("History") },
                    selected = false,
                    onClick = { navController.navigate("history") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Status Indicator
            StatusIndicator(state = deviceState)

            Spacer(modifier = Modifier.height(16.dp))

            // Device Name
            Text(
                text = deviceName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Text
            Text(
                text = when (deviceState) {
                    DeviceUiState.IN_RANGE -> "With you"
                    DeviceUiState.COUNTDOWN -> "Leaving..."
                    DeviceUiState.LEFT_BEHIND -> "Left behind!"
                    DeviceUiState.OFFLINE -> "Not tracking"
                    DeviceUiState.UNKNOWN -> "Scanning..."
                },
                style = MaterialTheme.typography.titleMedium,
                color = when (deviceState) {
                    DeviceUiState.IN_RANGE -> StatusGreen
                    DeviceUiState.COUNTDOWN -> StatusYellow
                    DeviceUiState.LEFT_BEHIND -> StatusRed
                    DeviceUiState.OFFLINE -> StatusGray
                    DeviceUiState.UNKNOWN -> StatusGray
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Last Seen
            Text(
                text = if (lastSeenText.isNotEmpty()) "Last seen: $lastSeenText" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (lastSeenLocation.isNotEmpty()) {
                Text(
                    text = lastSeenLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tracking Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isTracking) "Monitoring your tracker" else "Tracking paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTracking,
                        onCheckedChange = { enabled ->
                            // Persist the tracking preference so it survives restarts
                            viewModel.toggleTracking(enabled)
                            // Start/stop the tracking service
                            onToggleTracking(enabled)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Info Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Timer,
                    label = "Alert after",
                    value = "${viewModel.countdownSeconds}s",
                    color = StatusYellow
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Vibration,
                    label = "Vibration",
                    value = if (viewModel.vibrationEnabled) "On" else "Off",
                    color = StatusGreen
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.LocationOn,
                    label = "Location",
                    value = if (lastSeenLocation.isNotEmpty()) "Active" else "Pending",
                    color = if (lastSeenLocation.isNotEmpty()) StatusGreen else StatusGray
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(state: DeviceUiState) {
    val color by animateColorAsState(
        targetValue = when (state) {
            DeviceUiState.IN_RANGE -> StatusGreen
            DeviceUiState.COUNTDOWN -> StatusYellow
            DeviceUiState.LEFT_BEHIND -> StatusRed
            DeviceUiState.OFFLINE -> StatusGray
            DeviceUiState.UNKNOWN -> StatusGray
        },
        label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == DeviceUiState.LEFT_BEHIND) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp)
    ) {
        // Outer ring (pulsing for alerts)
        Box(
            modifier = Modifier
                .size((100 * scale).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
        )
        // Inner circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            // Core dot
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class DeviceUiState {
    IN_RANGE,
    COUNTDOWN,
    LEFT_BEHIND,
    OFFLINE,
    UNKNOWN
}
