package com.tagalert.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagalert.data.model.LocationHistory
import com.tagalert.data.repository.TagRepository
import com.tagalert.data.local.UserPreferences
import com.tagalert.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tagalert.ui.theme.*

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TagRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    val historyEntries: StateFlow<List<HistoryEntryUi>> = preferences.deviceId
        .flatMapLatest { deviceId ->
            if (deviceId.isNotEmpty()) {
                repository.getHistoryForDevice(deviceId)
            } else {
                repository.getRecentHistory()
            }
        }
        .map { entries -> entries.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun LocationHistory.toUi(): HistoryEntryUi {
        return HistoryEntryUi(
            title = when {
                wasLeftBehind && wasRecovered -> "Recovered"
                wasLeftBehind -> "Left behind"
                else -> "Seen"
            },
            subtitle = buildString {
                locationName?.let { append(it) }
                if (latitude != 0.0 || longitude != 0.0) {
                    if (isNotEmpty()) append(" · ")
                    append(String.format("%.4f, %.4f", latitude, longitude))
                }
                if (accuracy > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("±${String.format("%.0f", accuracy)}m")
                }
            },
            timeText = TimeUtils.formatTimestamp(timestamp),
            statusColor = when {
                wasLeftBehind && wasRecovered -> StatusGreen
                wasLeftBehind -> StatusRed
                else -> StatusYellow
            },
            statusIcon = when {
                wasLeftBehind && wasRecovered -> Icons.Outlined.CheckCircle
                wasLeftBehind -> Icons.Outlined.Warning
                else -> Icons.Outlined.LocationOn
            }
        )
    }
}
