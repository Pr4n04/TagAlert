package com.tagalert.data.model

/**
 * The current detection state of a tracked device.
 */
enum class DevicePresenceState {
    /** Device is within BLE range */
    IN_RANGE,
    /** Device has just gone out of range — countdown started */
    COUNTDOWN,
    /** Countdown expired — left behind alert triggered */
    LEFT_BEHIND,
    /** User acknowledged the alert */
    ACKNOWLEDGED,
    /** Device was lost but has been recovered */
    RECOVERED
}

/**
 * Represents a left-behind event.
 */
data class LeftBehindEvent(
    val deviceId: String,
    val deviceName: String,
    val lostTimestamp: Long,
    val lostLatitude: Double? = null,
    val lostLongitude: Double? = null,
    val lostLocationName: String? = null,
    val state: DevicePresenceState = DevicePresenceState.COUNTDOWN,
    val countdownSeconds: Int = 90
)
